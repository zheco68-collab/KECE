package com.kece.objective.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kece.common.exception.BusinessException;
import com.kece.common.response.PageResult;
import com.kece.common.response.ResultCode;
import com.kece.common.security.AuthContextHolder;
import com.kece.common.security.AuthUser;
import com.kece.common.security.Role;
import com.kece.objective.dto.AnswerResultVO;
import com.kece.objective.dto.AnswerSubmitRequest;
import com.kece.objective.dto.ObjectiveQuestionRequest;
import com.kece.objective.dto.ObjectiveQuestionVO;
import com.kece.objective.dto.ObjectiveSubmissionListVO;
import com.kece.objective.entity.ObjectiveQuestion;
import com.kece.objective.entity.ObjectiveSubmission;
import com.kece.objective.mapper.ObjectiveQuestionMapper;
import com.kece.objective.mapper.ObjectiveSubmissionMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 客观题业务逻辑。 */
@Service
@RequiredArgsConstructor
public class ObjectiveQuestionService {

    private final ObjectiveQuestionMapper mapper;
    private final ObjectiveSubmissionMapper submissionMapper;
    private final ObjectiveGrader grader;
    private final ObjectMapper objectMapper;

    /** 做题侧公开列表：只显示公开题。 */
    public PageResult<ObjectiveQuestionVO> list(String type, String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<ObjectiveQuestion> items = mapper.findPage(type, keyword, 1, null, offset, safeSize);
        long total = mapper.count(type, keyword, 1, null);
        return new PageResult<>(
                items.stream().map(q -> toVO(q, isPrivileged(q))).toList(),
                total,
                safePage,
                safeSize);
    }

    public ObjectiveQuestionVO detail(Long id) {
        ObjectiveQuestion question = getQuestionOrThrow(id);
        if (question.getStatus() != null && question.getStatus() != 1 && !canManage(question)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }
        return toVO(question, isPrivileged(question));
    }

    /** 后台客观题列表：ADMIN 可按 createdBy 筛选，其余角色只能看到自己出的题。 */
    public PageResult<ObjectiveQuestionVO> listAdmin(
            String type, String keyword, Integer status, Long createdBy, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        AuthUser user = AuthContextHolder.get();
        Long targetCreatedBy =
                (user != null && user.hasRole(Role.ADMIN))
                        ? createdBy
                        : (user == null ? null : user.getId());
        List<ObjectiveQuestion> items =
                mapper.findPage(type, keyword, status, targetCreatedBy, offset, safeSize);
        long total = mapper.count(type, keyword, status, targetCreatedBy);
        return new PageResult<>(
                items.stream().map(q -> toVO(q, isPrivileged(q))).toList(),
                total,
                safePage,
                safeSize);
    }

    @Transactional
    public ObjectiveQuestionVO create(ObjectiveQuestionRequest request) {
        ObjectiveQuestion question = new ObjectiveQuestion();
        applyRequest(question, request);
        question.setCreatedBy(AuthContextHolder.currentUserId());
        mapper.insert(question);
        return toVO(question, true);
    }

    @Transactional
    public ObjectiveQuestionVO update(Long id, ObjectiveQuestionRequest request) {
        ObjectiveQuestion question = getQuestionOrThrow(id);
        checkOwnerOrAdmin(question);
        applyRequest(question, request);
        mapper.update(question);
        return toVO(question, true);
    }

    @Transactional
    public void delete(Long id) {
        ObjectiveQuestion question = getQuestionOrThrow(id);
        checkOwnerOrAdmin(question);
        mapper.deleteById(id);
    }

    /** 作答并即时判分，同时留痕一条作答记录（可重复作答，逐次记录）。 */
    @Transactional
    public AnswerResultVO submit(Long id, AnswerSubmitRequest request) {
        ObjectiveQuestion question = getQuestionOrThrow(id);
        AnswerResultVO result = grader.grade(question, request.getAnswer());

        Long userId = AuthContextHolder.currentUserId();
        if (userId != null) {
            ObjectiveSubmission submission = new ObjectiveSubmission();
            submission.setUserId(userId);
            submission.setQuestionId(id);
            submission.setAnswer(request.getAnswer());
            submission.setCorrect(result.isCorrect() ? 1 : 0);
            submission.setScore(result.getScore());
            submissionMapper.insert(submission);
        }
        return result;
    }

    /** 当前用户已答对的客观题 id（去重），供列表标记「已通过」。 */
    public List<Long> solvedQuestionIds() {
        Long userId = AuthContextHolder.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return submissionMapper.findCorrectQuestionIds(userId);
    }

    /** 我的客观题作答记录：仅返回本人记录，按 id 倒序分页；correct 为 null 表示不筛对错。 */
    public PageResult<ObjectiveSubmissionListVO> listSubmissions(
            Integer correct, int page, int size) {
        Long userId = AuthContextHolder.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<ObjectiveSubmissionListVO> list =
                submissionMapper.findPage(userId, correct, offset, safeSize);
        long total = submissionMapper.count(userId, correct);
        return new PageResult<>(list, total, safePage, safeSize);
    }

    private ObjectiveQuestion getQuestionOrThrow(Long id) {
        ObjectiveQuestion question = mapper.findById(id);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }
        return question;
    }

    private void checkOwnerOrAdmin(ObjectiveQuestion question) {
        AuthUser user = AuthContextHolder.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (user.hasRole(Role.ADMIN) || question.getCreatedBy().equals(user.getId())) {
            return;
        }
        throw new BusinessException(ResultCode.FORBIDDEN);
    }

    /** 答案可见性：管理员或出题人本人。 */
    private boolean isPrivileged(ObjectiveQuestion question) {
        AuthUser user = AuthContextHolder.get();
        if (user == null) {
            return false;
        }
        if (user.hasRole(Role.ADMIN)) {
            return true;
        }
        return question.getCreatedBy().equals(user.getId());
    }

    private boolean canManage(ObjectiveQuestion question) {
        AuthUser user = AuthContextHolder.get();
        if (user == null) {
            return false;
        }
        if (user.hasRole(Role.ADMIN)) {
            return true;
        }
        return question.getCreatedBy().equals(user.getId());
    }

    private void applyRequest(ObjectiveQuestion question, ObjectiveQuestionRequest request) {
        question.setType(request.getType());
        question.setContent(request.getContent());
        question.setOptions(toOptionsJson(request.getOptions()));
        question.setAnswer(request.getAnswer());
        question.setMatchType(request.getMatchType() == null ? "EXACT" : request.getMatchType());
        question.setScore(request.getScore() == null ? 5 : request.getScore());
        question.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private ObjectiveQuestionVO toVO(ObjectiveQuestion question, boolean privileged) {
        return ObjectiveQuestionVO.builder()
                .id(question.getId())
                .type(question.getType())
                .content(question.getContent())
                .options(parseOptions(question.getOptions()))
                .answer(privileged ? question.getAnswer() : null)
                .matchType(question.getMatchType())
                .score(question.getScore())
                .createdBy(question.getCreatedBy())
                .status(question.getStatus())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    private String toOptionsJson(List<String> options) {
        try {
            return options == null ? null : objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "选项格式不正确");
        }
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
}
