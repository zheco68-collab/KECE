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
import com.kece.objective.entity.ObjectiveQuestion;
import com.kece.objective.mapper.ObjectiveQuestionMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 客观题业务逻辑。 */
@Service
@RequiredArgsConstructor
public class ObjectiveQuestionService {

    private final ObjectiveQuestionMapper mapper;
    private final ObjectiveGrader grader;
    private final ObjectMapper objectMapper;

    public PageResult<ObjectiveQuestionVO> list(String type, String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<ObjectiveQuestion> items = mapper.findPage(type, keyword, offset, safeSize);
        long total = mapper.count(type, keyword);
        boolean privileged = isPrivileged();
        return new PageResult<>(
                items.stream().map(q -> toVO(q, privileged)).toList(), total, safePage, safeSize);
    }

    public ObjectiveQuestionVO detail(Long id) {
        ObjectiveQuestion question = getQuestionOrThrow(id);
        return toVO(question, isPrivileged());
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

    public AnswerResultVO submit(Long id, AnswerSubmitRequest request) {
        ObjectiveQuestion question = getQuestionOrThrow(id);
        return grader.grade(question, request.getAnswer());
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

    private boolean isPrivileged() {
        AuthUser user = AuthContextHolder.get();
        return user != null && (user.hasRole(Role.TEACHER) || user.hasRole(Role.ADMIN));
    }

    private void applyRequest(ObjectiveQuestion question, ObjectiveQuestionRequest request) {
        question.setType(request.getType());
        question.setContent(request.getContent());
        question.setOptions(toOptionsJson(request.getOptions()));
        question.setAnswer(request.getAnswer());
        question.setMatchType(request.getMatchType() == null ? "EXACT" : request.getMatchType());
        question.setScore(request.getScore() == null ? 5 : request.getScore());
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
