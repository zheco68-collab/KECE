package com.kece.database.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kece.common.exception.BusinessException;
import com.kece.common.response.PageResult;
import com.kece.common.response.ResultCode;
import com.kece.common.security.AuthContextHolder;
import com.kece.common.security.AuthUser;
import com.kece.common.security.Role;
import com.kece.database.dto.DbSubmissionListVO;
import com.kece.database.entity.DbQuestion;
import com.kece.database.entity.DbSubmission;
import com.kece.database.entity.DbTestCase;
import com.kece.database.mapper.DbQuestionMapper;
import com.kece.database.mapper.DbSubmissionMapper;
import com.kece.database.mapper.DbTestCaseMapper;
import com.kece.submission.dto.ProgressVO;
import com.kece.submission.dto.SubmissionResultVO;
import com.kece.submission.dto.SubmissionVO;
import com.kece.submission.enums.SubmissionStatus;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 数据库提交与评测业务逻辑：压队列、Redis 状态缓存、结果落库。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbSubmissionService {

    private static final String QUEUE_KEY = "judge:sql:queue";
    private static final String RESULTS_KEY = "judge:sql:results";
    private static final String STATUS_KEY_PREFIX = "dbsub:status:";
    private static final String OWNER_KEY_PREFIX = "dbsub:owner:";
    private static final long STATUS_TTL_SECONDS = 3600;
    private static final int SQL_TIME_LIMIT_MS = 5000;

    private final DbSubmissionMapper submissionMapper;
    private final DbQuestionMapper questionMapper;
    private final DbTestCaseMapper testCaseMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /** 提交 SQL：入库、压队列、写 Redis 初始状态，返回任务 ID 与 PENDING。 */
    @Transactional
    public SubmissionVO submit(Long questionId, String sql) {
        Long userId = AuthContextHolder.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String trimmed = sql == null ? "" : sql.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "SQL 不能为空");
        }
        DbQuestion question = questionMapper.findById(questionId);
        if (question == null || question.getStatus() == null || question.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在或未公开");
        }

        DbSubmission submission = new DbSubmission();
        submission.setUserId(userId);
        submission.setQuestionId(questionId);
        submission.setDialect(question.getDialect());
        submission.setSqlContent(trimmed);
        submission.setStatus(SubmissionStatus.PENDING.name());
        submissionMapper.insert(submission);

        List<DbTestCase> testCases = testCaseMapper.findByQuestionId(questionId);
        String taskJson = writeJson(buildTask(submission, question, testCases, trimmed));

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

    /** 数据库提交记录列表：无论何种用户类型，均仅返回本人提交，按 id 倒序分页。 */
    public PageResult<DbSubmissionListVO> list(String status, int page, int size) {
        AuthUser user = AuthContextHolder.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Long userId = user.getId();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        String statusFilter =
                (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        List<DbSubmissionListVO> list =
                submissionMapper.findPage(userId, statusFilter, offset, safeSize);
        long total = submissionMapper.count(userId, statusFilter);
        return new PageResult<>(list, total, safePage, safeSize);
    }

    /** 当前用户已 AC 的数据库题目 id（去重），供题目列表标记「已通过」。 */
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
        DbSubmission submission = submissionMapper.findById(id);
        if (submission == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提交记录不存在");
        }
        if (!canView(user, submission)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return sanitize(toVOFromEntity(submission), user);
    }

    /**
     * 隐藏逐用例的期望/实际输出，避免答题者由比对结果反推隐藏测试点的数据与答案。
     *
     * <p>样例测试点的数据本就随题面公开（题目详情的「样例数据」），因此保留其比对结果供自助排查； 非样例测试点一律置空。管理员不受限。
     */
    private SubmissionVO sanitize(SubmissionVO vo, AuthUser user) {
        if (vo == null || user.hasRole(Role.ADMIN) || vo.getResult() == null) {
            return vo;
        }
        var report = vo.getResult().getTestCasesReport();
        if (report == null || report.isEmpty()) {
            return vo;
        }
        Set<Long> sampleCaseIds = findSampleCaseIds(vo.getSubmissionId());
        report.forEach(
                c -> {
                    if (sampleCaseIds.contains(c.getCaseId())) {
                        return;
                    }
                    c.setExpectedOutput(null);
                    c.setActualOutput(null);
                });
        return vo;
    }

    /** 该提交所属题目的样例测试点 id；题目已删除或查不到时返回空集合（等价于全部隐藏）。 */
    private Set<Long> findSampleCaseIds(Long submissionId) {
        DbSubmission submission =
                submissionId == null ? null : submissionMapper.findById(submissionId);
        if (submission == null) {
            return Set.of();
        }
        List<Long> ids = testCaseMapper.findSampleCaseIds(submission.getQuestionId());
        return ids == null || ids.isEmpty() ? Set.of() : new HashSet<>(ids);
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
                    log.warn("回写数据库评测结果失败 submissionId={}: {}", submissionIdStr, e.getMessage());
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
                result == null ? null : writeJson(result));
    }

    private Map<String, Object> buildTask(
            DbSubmission submission, DbQuestion question, List<DbTestCase> testCases, String sql) {
        Map<String, Object> task = new HashMap<>();
        task.put("submissionId", submission.getId());
        task.put("questionId", submission.getQuestionId());
        task.put("type", "sql");
        task.put("dialect", question.getDialect());
        // 建表语句与标准答案在题目级，所有测试点共用；测试点只带各自的一套数据。
        task.put("schemaSql", question.getSchemaSql());
        task.put("referenceSql", question.getReferenceSql());
        task.put("verifySql", question.getVerifySql());
        task.put(
                "orderSensitive",
                question.getOrderSensitive() != null && question.getOrderSensitive() == 1);
        task.put("codeContent", Map.of("answer.sql", sql));
        task.put("timeLimitMs", SQL_TIME_LIMIT_MS);
        List<Map<String, Object>> cases =
                testCases.stream()
                        .map(
                                tc -> {
                                    Map<String, Object> c = new HashMap<>();
                                    c.put("caseId", tc.getId());
                                    c.put("seedSql", tc.getSeedSql());
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

    private SubmissionVO toVOFromEntity(DbSubmission s) {
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

    private boolean canView(AuthUser user, DbSubmission submission) {
        if (user.hasRole(Role.ADMIN)) {
            return true;
        }
        return submission.getUserId().equals(user.getId());
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
