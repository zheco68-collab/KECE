package com.kece.exam.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kece.common.exception.BusinessException;
import com.kece.common.response.PageResult;
import com.kece.common.response.ResultCode;
import com.kece.common.security.AuthContextHolder;
import com.kece.common.security.AuthUser;
import com.kece.common.security.Role;
import com.kece.database.entity.DbQuestion;
import com.kece.database.mapper.DbQuestionMapper;
import com.kece.database.mapper.DbSubmissionMapper;
import com.kece.exam.dto.ExamQuestionRequest;
import com.kece.exam.dto.ExamQuestionVO;
import com.kece.exam.dto.ExamRequest;
import com.kece.exam.dto.ExamSessionQuestionVO;
import com.kece.exam.dto.ExamSessionVO;
import com.kece.exam.dto.ExamVO;
import com.kece.exam.dto.SessionEnterVO;
import com.kece.exam.entity.Exam;
import com.kece.exam.entity.ExamQuestion;
import com.kece.exam.entity.ExamSessionStatus;
import com.kece.exam.mapper.ExamMapper;
import com.kece.exam.mapper.ExamQuestionMapper;
import com.kece.exam.mapper.ExamSubmissionMapper;
import com.kece.objective.entity.ObjectiveQuestion;
import com.kece.objective.mapper.ObjectiveQuestionMapper;
import com.kece.question.entity.Question;
import com.kece.question.mapper.QuestionMapper;
import com.kece.submission.mapper.SubmissionMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 考试业务逻辑：CRUD、组卷、进入/列表/取消（考试本身即一次考试，无场次中间层）。 */
@Service
@RequiredArgsConstructor
public class ExamService {

    private static final Set<String> VALID_MODES = Set.of("ACM", "OI");
    private static final int TITLE_MAX_LENGTH = 255;

    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamSubmissionMapper submissionMapper;
    private final SubmissionMapper codeSubmissionMapper;
    private final DbSubmissionMapper dbSubmissionMapper;
    private final QuestionMapper questionMapper;
    private final DbQuestionMapper dbQuestionMapper;
    private final ObjectiveQuestionMapper objectiveQuestionMapper;
    private final ObjectMapper objectMapper;

    /** 公开列表：仅公开考试；all=true 时特权角色可见私有考试。 */
    public PageResult<ExamVO> list(
            String keyword, Integer status, int page, int size, boolean all) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        Integer effectiveStatus = (all && isPrivileged()) ? status : 1;
        List<Exam> items = examMapper.findPage(keyword, effectiveStatus, null, offset, safeSize);
        long total = examMapper.count(keyword, effectiveStatus, null);

        AuthUser currentUser = AuthContextHolder.get();
        Set<Long> submittedExamIds = Collections.emptySet();
        if (currentUser != null) {
            submittedExamIds =
                    new HashSet<>(
                            submissionMapper.findSubmittedExamIdsByUserId(currentUser.getId()));
        }
        Set<Long> finalSubmittedExamIds = submittedExamIds;

        return new PageResult<>(
                items.stream()
                        .map(e -> toVO(e, false, finalSubmittedExamIds.contains(e.getId())))
                        .toList(),
                total,
                safePage,
                safeSize);
    }

    /** 后台列表：ADMIN 可按 createdBy 筛选，其余角色仅见自己创建的考试。 */
    public PageResult<ExamVO> listAdmin(
            String keyword, Integer status, Long createdBy, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        AuthUser user = AuthContextHolder.get();
        Long targetCreatedBy =
                (user != null && user.hasRole(Role.ADMIN))
                        ? createdBy
                        : (user == null ? null : user.getId());
        List<Exam> items = examMapper.findPage(keyword, status, targetCreatedBy, offset, safeSize);
        long total = examMapper.count(keyword, status, targetCreatedBy);

        Set<Long> submittedExamIds = Collections.emptySet();
        if (user != null) {
            submittedExamIds =
                    new HashSet<>(submissionMapper.findSubmittedExamIdsByUserId(user.getId()));
        }
        Set<Long> finalSubmittedExamIds = submittedExamIds;

        return new PageResult<>(
                items.stream()
                        .map(e -> toVO(e, false, finalSubmittedExamIds.contains(e.getId())))
                        .toList(),
                total,
                safePage,
                safeSize);
    }

    public ExamVO detail(Long id) {
        Exam exam = getExamOrThrow(id);
        if (exam.getStatus() != 1 && !canManage(exam)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "考试不存在");
        }
        AuthUser user = AuthContextHolder.get();
        boolean submitted = false;
        if (user != null) {
            submitted = submissionMapper.countSubmittedByExamAndUser(id, user.getId()) > 0;
        }
        return toVO(exam, true, submitted);
    }

    @Transactional
    public ExamVO create(ExamRequest request) {
        validate(request);
        Exam exam = new Exam();
        applyRequest(exam, request);
        // 确定考试起止时间：startTime 为空则立即开始，endTime = startTime + duration
        LocalDateTime start =
                request.getStartTime() == null ? LocalDateTime.now() : request.getStartTime();
        LocalDateTime end = start.plusMinutes(exam.getDurationMinutes());
        exam.setStartTime(start);
        exam.setEndTime(end);
        exam.setSessionStatus(ExamSessionStatus.UPCOMING.name());
        exam.setCreatedBy(AuthContextHolder.currentUserId());
        examMapper.insert(exam);
        snapshotQuestions(exam.getId(), request.getQuestions());
        return detail(exam.getId());
    }

    @Transactional
    public ExamVO update(Long id, ExamRequest request) {
        Exam exam = getExamOrThrow(id);
        checkOwnerOrAdmin(exam);
        if (submissionMapper.countByExam(id) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该考试已有作答记录，题目不可再修改");
        }
        validate(request);
        applyRequest(exam, request);
        // 更新起止时间
        LocalDateTime start =
                request.getStartTime() == null ? LocalDateTime.now() : request.getStartTime();
        LocalDateTime end = start.plusMinutes(exam.getDurationMinutes());
        exam.setStartTime(start);
        exam.setEndTime(end);
        examMapper.update(exam);
        examQuestionMapper.deleteByExamId(id);
        snapshotQuestions(id, request.getQuestions());
        return detail(id);
    }

    /** 删除考试：无作答记录时出题人/管理员均可删；有作答记录时仅 ADMIN 可强制删除（级联清理作答与评测提交）。 */
    @Transactional
    public void delete(Long id) {
        Exam exam = getExamOrThrow(id);
        checkOwnerOrAdmin(exam);
        AuthUser user = AuthContextHolder.get();
        boolean admin = user != null && user.hasRole(Role.ADMIN);
        if (submissionMapper.countByExam(id) > 0 && !admin) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该考试已有作答记录，仅管理员可强制删除");
        }
        // ADMIN 强制删除：级联清理考试作答与考试内产生的代码/数据库评测提交
        if (admin) {
            submissionMapper.deleteByExam(id);
            codeSubmissionMapper.deleteByContest(id);
            dbSubmissionMapper.deleteByContest(id);
        }
        examQuestionMapper.deleteByExamId(id);
        examMapper.deleteById(id);
    }

    // ==================== 考生侧：进入 / 公开列表 / 取消 ====================

    /** 考生进入考试：状态机惰性推进，返回倒计时与题目快照。 */
    public SessionEnterVO enter(Long id) {
        Exam exam = getExamOrThrow(id);
        ExamSessionStatus status = advanceStatus(exam);
        if (status == ExamSessionStatus.CANCELLED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该考试已取消");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "考试尚未开始");
        }
        if (status == ExamSessionStatus.ENDED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "考试已结束，已自动交卷");
        }
        AuthUser currentUser = AuthContextHolder.get();
        if (currentUser != null
                && submissionMapper.countSubmittedByExamAndUser(id, currentUser.getId()) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "您已交卷，无法再次进入考场，请查看战报");
        }
        long remainingSeconds = Duration.between(now, exam.getEndTime()).getSeconds();
        List<ExamSessionQuestionVO> questions =
                examQuestionMapper.findByExamId(exam.getId()).stream()
                        .map(this::buildSessionQuestion)
                        .toList();
        return SessionEnterVO.builder()
                .id(exam.getId())
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .mode(exam.getMode())
                .durationMinutes(exam.getDurationMinutes())
                .startTime(exam.getStartTime())
                .endTime(exam.getEndTime())
                .status(exam.getSessionStatus())
                .remainingSeconds(Math.max(remainingSeconds, 0))
                .questions(questions)
                .build();
    }

    /** 公开可进入考试列表（公开且未结束/未取消）。 */
    public PageResult<ExamSessionVO> listOpen(String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<Exam> items = examMapper.findPage(keyword, 1, null, offset, safeSize);
        // 过滤掉已结束/已取消的（状态需惰性推进后判断，但列表场景直接按时间窗口过滤）
        LocalDateTime now = LocalDateTime.now();
        List<ExamSessionVO> list =
                items.stream()
                        .filter(
                                e -> {
                                    if (ExamSessionStatus.CANCELLED
                                            .name()
                                            .equals(e.getSessionStatus())) {
                                        return false;
                                    }
                                    // 未取消且结束时间在未来或刚到
                                    return e.getEndTime() == null
                                            || !now.isAfter(e.getEndTime().plusMinutes(1));
                                })
                        .map(this::toSessionVO)
                        .toList();
        long total = examMapper.count(keyword, 1, null);
        return new PageResult<>(list, total, safePage, safeSize);
    }

    /** 取消考试（有作答记录则不可取消）。 */
    public void cancel(Long id) {
        Exam exam = getExamOrThrow(id);
        checkOwnerOrAdmin(exam);
        ExamSessionStatus status = advanceStatus(exam);
        if (status == ExamSessionStatus.ENDED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "考试已结束，不能取消");
        }
        if (submissionMapper.countByExam(id) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该考试已有作答记录，不能取消");
        }
        examMapper.updateSessionStatus(id, ExamSessionStatus.CANCELLED.name());
    }

    // ==================== 内部方法 ====================

    private void validate(ExamRequest request) {
        String mode = request.getMode().trim().toUpperCase();
        if (!VALID_MODES.contains(mode)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "评分模式不合法（ACM/OI）");
        }
        if (request.getStatus() != null && request.getStatus() != 0 && request.getStatus() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "状态不合法");
        }
    }

    private void applyRequest(Exam exam, ExamRequest request) {
        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setMode(request.getMode().trim().toUpperCase());
        exam.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    /** 组卷快照：按请求顺序写入快照条目，题目可见性校验在服务端强制。 */
    private void snapshotQuestions(Long examId, List<ExamQuestionRequest> questions) {
        int order = 0;
        for (ExamQuestionRequest req : questions) {
            ExamQuestion snapshot = buildSnapshot(examId, order++, req);
            examQuestionMapper.insert(snapshot);
        }
    }

    private ExamQuestion buildSnapshot(Long examId, int sortOrder, ExamQuestionRequest req) {
        String type = req.getQuestionType().trim().toUpperCase();
        String title;
        switch (type) {
            case "ALGO" -> {
                Question q = questionMapper.findById(req.getQuestionId());
                title =
                        visibleTitle(
                                q == null ? null : q.getTitle(),
                                q == null ? null : q.getStatus(),
                                q == null ? null : q.getCreatedBy());
            }
            case "DB" -> {
                DbQuestion q = dbQuestionMapper.findById(req.getQuestionId());
                title =
                        visibleTitle(
                                q == null ? null : q.getTitle(),
                                q == null ? null : q.getStatus(),
                                q == null ? null : q.getCreatedBy());
            }
            case "OBJECTIVE" -> {
                ObjectiveQuestion q = objectiveQuestionMapper.findById(req.getQuestionId());
                title =
                        visibleTitle(
                                q == null ? null : q.getContent(),
                                q == null ? null : q.getStatus(),
                                q == null ? null : q.getCreatedBy());
            }
            default ->
                    throw new BusinessException(ResultCode.PARAM_ERROR, "题型不合法（ALGO/DB/OBJECTIVE）");
        }
        if (title == null) {
            throw new BusinessException(
                    ResultCode.PARAM_ERROR, "题目不存在或不可组卷: " + type + "#" + req.getQuestionId());
        }
        ExamQuestion snapshot = new ExamQuestion();
        snapshot.setExamId(examId);
        snapshot.setQuestionId(req.getQuestionId());
        snapshot.setQuestionType(type);
        snapshot.setTitle(truncate(title, TITLE_MAX_LENGTH));
        snapshot.setScore(req.getScore());
        snapshot.setSortOrder(sortOrder);
        return snapshot;
    }

    private String visibleTitle(String title, Integer questionStatus, Long createdBy) {
        if (title == null || questionStatus == null) {
            return null;
        }
        if (questionStatus == 1) {
            return title;
        }
        AuthUser user = AuthContextHolder.get();
        if (user != null && (user.hasRole(Role.ADMIN) || createdBy.equals(user.getId()))) {
            return title;
        }
        return null;
    }

    private ExamSessionQuestionVO buildSessionQuestion(ExamQuestion q) {
        ExamSessionQuestionVO.ExamSessionQuestionVOBuilder b =
                ExamSessionQuestionVO.builder()
                        .id(q.getId())
                        .questionId(q.getQuestionId())
                        .questionType(q.getQuestionType())
                        .title(q.getTitle())
                        .score(q.getScore())
                        .sortOrder(q.getSortOrder());
        switch (q.getQuestionType()) {
            case "ALGO" -> {
                Question question = questionMapper.findById(q.getQuestionId());
                if (question != null) {
                    b.content(question.getDescription())
                            .timeLimitMs(question.getTimeLimit())
                            .memoryLimitMb(question.getMemoryLimit());
                }
            }
            case "DB" -> {
                DbQuestion question = dbQuestionMapper.findById(q.getQuestionId());
                if (question != null) {
                    b.content(question.getDescription()).schemaSql(question.getSchemaSql());
                }
            }
            case "OBJECTIVE" -> {
                ObjectiveQuestion question = objectiveQuestionMapper.findById(q.getQuestionId());
                if (question != null) {
                    b.content(question.getContent())
                            .objectiveType(question.getType())
                            .options(parseOptions(question.getOptions()));
                }
            }
            default -> {}
        }
        return b.build();
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 状态机推进：UPCOMING→ONGOING→ENDED 按当前时间判定，变化时持久化并返回最新状态。 */
    private ExamSessionStatus advanceStatus(Exam exam) {
        LocalDateTime now = LocalDateTime.now();
        ExamSessionStatus current =
                exam.getSessionStatus() == null
                        ? ExamSessionStatus.UPCOMING
                        : ExamSessionStatus.valueOf(exam.getSessionStatus());
        ExamSessionStatus target;
        if (current == ExamSessionStatus.CANCELLED) {
            return current;
        }
        if (exam.getEndTime() != null && !now.isBefore(exam.getEndTime())) {
            target = ExamSessionStatus.ENDED;
        } else if (exam.getStartTime() != null && !now.isBefore(exam.getStartTime())) {
            target = ExamSessionStatus.ONGOING;
        } else {
            target = ExamSessionStatus.UPCOMING;
        }
        if (current != target) {
            exam.setSessionStatus(target.name());
            examMapper.updateSessionStatus(exam.getId(), target.name());
        }
        return target;
    }

    private Exam getExamOrThrow(Long id) {
        Exam exam = examMapper.findById(id);
        if (exam == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "考试不存在");
        }
        return exam;
    }

    private void checkOwnerOrAdmin(Exam exam) {
        AuthUser user = AuthContextHolder.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (user.hasRole(Role.ADMIN) || exam.getCreatedBy().equals(user.getId())) {
            return;
        }
        throw new BusinessException(ResultCode.FORBIDDEN);
    }

    private boolean canManage(Exam exam) {
        AuthUser user = AuthContextHolder.get();
        if (user == null) {
            return false;
        }
        if (user.hasRole(Role.ADMIN)) {
            return true;
        }
        return exam.getCreatedBy().equals(user.getId());
    }

    private boolean isPrivileged() {
        AuthUser user = AuthContextHolder.get();
        return user != null && (user.hasRole(Role.TEACHER) || user.hasRole(Role.ADMIN));
    }

    private ExamVO toVO(Exam exam, boolean withQuestions) {
        return toVO(exam, withQuestions, null);
    }

    private ExamVO toVO(Exam exam, boolean withQuestions, Boolean submitted) {
        advanceStatus(exam);
        List<ExamQuestionVO> questions =
                withQuestions
                        ? examQuestionMapper.findByExamId(exam.getId()).stream()
                                .map(this::toQuestionVO)
                                .toList()
                        : List.of();
        return ExamVO.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .durationMinutes(exam.getDurationMinutes())
                .startTime(exam.getStartTime())
                .endTime(exam.getEndTime())
                .mode(exam.getMode())
                .status(exam.getStatus())
                .sessionStatus(exam.getSessionStatus())
                .submitted(submitted)
                .createdBy(exam.getCreatedBy())
                .questions(questions)
                .createdAt(exam.getCreatedAt())
                .updatedAt(exam.getUpdatedAt())
                .build();
    }

    private ExamSessionVO toSessionVO(Exam exam) {
        advanceStatus(exam);
        return ExamSessionVO.builder()
                .id(exam.getId())
                .examId(exam.getId())
                .examTitle(exam.getTitle())
                .startTime(exam.getStartTime())
                .endTime(exam.getEndTime())
                .durationMinutes(exam.getDurationMinutes())
                .status(exam.getSessionStatus())
                .createdBy(exam.getCreatedBy())
                .createdAt(exam.getCreatedAt())
                .build();
    }

    private ExamQuestionVO toQuestionVO(ExamQuestion q) {
        return ExamQuestionVO.builder()
                .id(q.getId())
                .questionId(q.getQuestionId())
                .questionType(q.getQuestionType())
                .title(q.getTitle())
                .score(q.getScore())
                .sortOrder(q.getSortOrder())
                .build();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
