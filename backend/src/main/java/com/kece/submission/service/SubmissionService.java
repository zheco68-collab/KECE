package com.kece.submission.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kece.common.exception.BusinessException;
import com.kece.common.response.PageResult;
import com.kece.common.response.ResultCode;
import com.kece.common.security.AuthContextHolder;
import com.kece.common.security.AuthUser;
import com.kece.common.security.Role;
import com.kece.question.entity.Question;
import com.kece.question.entity.TestCase;
import com.kece.question.mapper.QuestionMapper;
import com.kece.question.mapper.TestCaseMapper;
import com.kece.submission.dto.ProgressVO;
import com.kece.submission.dto.SubmissionListVO;
import com.kece.submission.dto.SubmissionResultVO;
import com.kece.submission.dto.SubmissionVO;
import com.kece.submission.dto.SubmitRequest;
import com.kece.submission.entity.Submission;
import com.kece.submission.enums.SubmissionStatus;
import com.kece.submission.mapper.SubmissionMapper;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 提交与评测业务逻辑：压队列、Redis 状态缓存、结果落库。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private static final String QUEUE_KEY = "judge:queue";
    private static final String RESULTS_KEY = "judge:results";
    private static final String STATUS_KEY_PREFIX = "sub:status:";
    private static final String OWNER_KEY_PREFIX = "sub:owner:";
    private static final long STATUS_TTL_SECONDS = 3600;
    private static final List<String> SUPPORTED_LANGUAGES = List.of("cpp", "java", "python");

    private final SubmissionMapper submissionMapper;
    private final QuestionMapper questionMapper;
    private final TestCaseMapper testCaseMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /** 提交代码：入库、压队列、写 Redis 初始状态，返回任务 ID 与 PENDING。 */
    @Transactional
    public SubmissionVO submit(SubmitRequest request) {
        Long userId = AuthContextHolder.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String language = request.getLanguage().trim().toLowerCase();
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的语言: " + request.getLanguage());
        }
        Question question = questionMapper.findById(request.getQuestionId());
        if (question == null || question.getStatus() == null || question.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在或未公开");
        }
        if (!supportsLanguage(question, language)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该题目不支持此语言");
        }
        Map<String, String> code = normalizeCode(request.getCodeContent());
        if (code.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "代码内容不能为空");
        }

        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setQuestionId(request.getQuestionId());
        submission.setContestId(request.getContestId());
        submission.setLanguage(language);
        submission.setCodeContent(writeJson(code));
        submission.setStatus(SubmissionStatus.PENDING.name());
        submissionMapper.insert(submission);

        List<TestCase> testCases = testCaseMapper.findByQuestionId(request.getQuestionId());
        String taskJson = writeJson(buildTask(submission, question, testCases, code));

        SubmissionVO pending = buildPendingVO(submission.getId(), testCases.size());
        redis.opsForValue()
                .set(
                        statusKey(submission.getId()),
                        writeJson(pending),
                        Duration.ofSeconds(STATUS_TTL_SECONDS));
        redis.opsForValue()
                .set(
                        ownerKey(submission.getId()),
                        String.valueOf(userId),
                        Duration.ofSeconds(STATUS_TTL_SECONDS));
        redis.opsForList().rightPush(QUEUE_KEY, taskJson);

        return pending;
    }

    /** 我的提交列表：无论何种用户类型，均仅返回本人提交，按 id 倒序分页。 */
    public PageResult<SubmissionListVO> list(String status, int page, int size) {
        AuthUser user = AuthContextHolder.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Long userId = user.getId();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        String statusFilter = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        List<SubmissionListVO> list = submissionMapper.findPage(userId, statusFilter, offset, safeSize);
        long total = submissionMapper.count(userId, statusFilter);
        return new PageResult<>(list, total, safePage, safeSize);
    }

    /** 当前用户已 AC 的题目 id（去重），供题目列表标记「已通过」。 */
    public List<Long> solvedQuestionIds() {
        Long userId = AuthContextHolder.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return submissionMapper.findAcceptedQuestionIds(userId);
    }

    /** 查询评测状态与结果：优先命中 Redis 状态缓存，缺失时回退 MySQL。 */
    public SubmissionVO get(Long id) {
        AuthUser user = AuthContextHolder.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String ownerId = redis.opsForValue().get(ownerKey(id));
        if (ownerId != null) {
            checkOwner(user, ownerId);
            String cached = redis.opsForValue().get(statusKey(id));
            if (cached != null) {
                return sanitize(readJson(cached, SubmissionVO.class), user);
            }
        }
        Submission submission = submissionMapper.findById(id);
        if (submission == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提交记录不存在");
        }
        if (!canView(user, submission)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return sanitize(toVOFromEntity(submission), user);
    }

    /** 对非管理员答题者隐藏逐用例的期望/实际输出（隐藏用例内容），仅保留各节点状态。 */
    private SubmissionVO sanitize(SubmissionVO vo, AuthUser user) {
        if (vo == null || user.hasRole(Role.ADMIN) || vo.getResult() == null) {
            return vo;
        }
        var report = vo.getResult().getTestCasesReport();
        if (report != null) {
            report.forEach(c -> {
                c.setExpectedOutput(null);
                c.setActualOutput(null);
            });
        }
        return vo;
    }

    /** 定时回写：将判题机写入 Redis 的终态结果落库 MySQL（幂等）。 */
    @Scheduled(fixedDelay = 2000)
    public void collectResults() {
        try {
            while (true) {
                String submissionIdStr = redis.opsForList().leftPop(RESULTS_KEY);
                if (submissionIdStr == null) {
                    break;
                }
                try {
                    persistResult(Long.valueOf(submissionIdStr));
                } catch (Exception e) {
                    log.warn("回写评测结果失败 submissionId={}: {}", submissionIdStr, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("回写任务扫描跳过: {}", e.getMessage());
        }
    }

    private void persistResult(Long submissionId) {
        String cached = redis.opsForValue().get(statusKey(submissionId));
        if (cached == null) {
            return;
        }
        SubmissionVO vo = readJson(cached, SubmissionVO.class);
        if (vo == null
                || vo.getStatus() == null
                || !SubmissionStatus.from(vo.getStatus()).isTerminal()) {
            return;
        }
        SubmissionResultVO result = vo.getResult();
        submissionMapper.updateResult(
                submissionId,
                vo.getStatus(),
                result == null ? null : result.getScore(),
                result == null ? null : result.getTimeUsedMs(),
                result == null ? null : result.getMemoryUsedKb(),
                result == null ? null : result.getCompileErrorLog(),
                result == null ? null : writeJson(result));
    }

    private Map<String, Object> buildTask(
            Submission submission,
            Question question,
            List<TestCase> testCases,
            Map<String, String> code) {
        Map<String, Object> task = new HashMap<>();
        task.put("submissionId", submission.getId());
        task.put("questionId", submission.getQuestionId());
        task.put("language", submission.getLanguage());
        task.put("codeContent", code);
        task.put("timeLimitMs", question.getTimeLimit() == null ? 1000 : question.getTimeLimit());
        task.put(
                "memoryLimitMb",
                question.getMemoryLimit() == null ? 256 : question.getMemoryLimit());
        List<Map<String, Object>> cases =
                testCases.stream()
                        .map(
                                tc -> {
                                    Map<String, Object> c = new HashMap<>();
                                    c.put("caseId", tc.getId());
                                    c.put("input", tc.getInput());
                                    c.put("expectedOutput", tc.getExpectedOutput());
                                    return c;
                                })
                        .toList();
        task.put("testCases", cases);
        return task;
    }

    private SubmissionVO buildPendingVO(Long id, int totalCases) {
        return SubmissionVO.builder()
                .submissionId(id)
                .status(SubmissionStatus.PENDING.name())
                .progress(ProgressVO.builder().totalCases(totalCases).finishedCases(0).build())
                .build();
    }

    private SubmissionVO toVOFromEntity(Submission s) {
        SubmissionResultVO result = null;
        if (s.getResultJson() != null && !s.getResultJson().isBlank()) {
            result = readJson(s.getResultJson(), SubmissionResultVO.class);
        }
        return SubmissionVO.builder()
                .submissionId(s.getId())
                .status(s.getStatus())
                .result(result)
                .build();
    }

    private void checkOwner(AuthUser user, String ownerId) {
        if (user.hasRole(Role.ADMIN)) {
            return;
        }
        if (!ownerId.equals(String.valueOf(user.getId()))) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private boolean canView(AuthUser user, Submission submission) {
        if (user.hasRole(Role.ADMIN)) {
            return true;
        }
        return submission.getUserId().equals(user.getId());
    }

    private boolean supportsLanguage(Question question, String language) {
        if (question.getLanguages() == null || question.getLanguages().isBlank()) {
            return true;
        }
        return Arrays.stream(question.getLanguages().split(","))
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .anyMatch(l -> l.equalsIgnoreCase(language));
    }

    private Map<String, String> normalizeCode(Map<String, String> codeContent) {
        if (codeContent == null) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        codeContent.forEach(
                (k, v) -> {
                    if (k != null && !k.isBlank() && v != null && !v.isBlank()) {
                        result.put(k.trim(), v);
                    }
                });
        return result;
    }

    private String statusKey(Long id) {
        return STATUS_KEY_PREFIX + id;
    }

    private String ownerKey(Long id) {
        return OWNER_KEY_PREFIX + id;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "JSON 序列化失败");
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
