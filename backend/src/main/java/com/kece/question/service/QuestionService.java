package com.kece.question.service;

import com.kece.common.exception.BusinessException;
import com.kece.common.response.PageResult;
import com.kece.common.response.ResultCode;
import com.kece.common.security.AuthContextHolder;
import com.kece.common.security.AuthUser;
import com.kece.common.security.Role;
import com.kece.question.dto.QuestionRequest;
import com.kece.question.dto.QuestionVO;
import com.kece.question.dto.TestCaseRequest;
import com.kece.question.dto.TestCaseVO;
import com.kece.question.entity.Question;
import com.kece.question.entity.TestCase;
import com.kece.question.mapper.QuestionMapper;
import com.kece.question.mapper.TestCaseMapper;
import com.kece.submission.mapper.SubmissionMapper;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 算法题库业务逻辑。 */
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final TestCaseMapper testCaseMapper;
    private final SubmissionMapper submissionMapper;

    public PageResult<QuestionVO> list(
            String keyword, String difficulty, int page, int size, boolean all) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        Integer status = (all && isPrivileged()) ? null : 1;
        List<Question> questions =
                questionMapper.findPage(keyword, difficulty, status, null, offset, safeSize);
        long total = questionMapper.count(keyword, difficulty, status, null);
        return new PageResult<>(
                questions.stream().map(this::toVO).toList(), total, safePage, safeSize);
    }

    /** 后台算法题列表：ADMIN 可按 createdBy 筛选，非 ADMIN 仅见自己出的题。 */
    public PageResult<QuestionVO> listAdmin(
            String keyword, String difficulty, Integer status, Long createdBy, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        Long targetCreatedBy = createdBy;
        AuthUser user = AuthContextHolder.get();
        if (user == null || !user.hasRole(Role.ADMIN)) {
            targetCreatedBy = AuthContextHolder.currentUserId();
        }
        List<Question> questions =
                questionMapper.findPage(
                        keyword, difficulty, status, targetCreatedBy, offset, safeSize);
        long total = questionMapper.count(keyword, difficulty, status, targetCreatedBy);
        return new PageResult<>(
                questions.stream().map(this::toVO).toList(), total, safePage, safeSize);
    }

    public QuestionVO detail(Long id) {
        Question question = getQuestionOrThrow(id);
        if (question.getStatus() != 1 && !canManage(question)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }
        return toVO(question);
    }

    @Transactional
    public QuestionVO create(QuestionRequest request) {
        Question question = new Question();
        applyRequest(question, request);
        question.setCreatedBy(AuthContextHolder.currentUserId());
        questionMapper.insert(question);
        return toVO(question);
    }

    @Transactional
    public QuestionVO update(Long id, QuestionRequest request) {
        Question question = getQuestionOrThrow(id);
        checkOwnerOrAdmin(question);
        applyRequest(question, request);
        questionMapper.update(question);
        return toVO(question);
    }

    @Transactional
    public void delete(Long id) {
        Question question = getQuestionOrThrow(id);
        checkOwnerOrAdmin(question);
        if (submissionMapper.existsByQuestionId(id)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该题目已有提交记录，不能删除");
        }
        testCaseMapper.deleteByQuestionId(id);
        questionMapper.deleteById(id);
    }

    public List<TestCaseVO> listTestCases(Long questionId) {
        Question question = getQuestionOrThrow(questionId);
        checkOwnerOrAdmin(question);
        return testCaseMapper.findByQuestionId(questionId).stream()
                .map(this::toTestCaseVO)
                .toList();
    }

    @Transactional
    public TestCaseVO addTestCase(Long questionId, TestCaseRequest request) {
        Question question = getQuestionOrThrow(questionId);
        checkOwnerOrAdmin(question);
        TestCase testCase = new TestCase();
        testCase.setQuestionId(questionId);
        testCase.setInput(request.getInput());
        testCase.setExpectedOutput(request.getExpectedOutput());
        testCase.setIsSample(request.getIsSample() == null ? 0 : request.getIsSample());
        testCaseMapper.insert(testCase);
        return toTestCaseVO(testCase);
    }

    @Transactional
    public TestCaseVO updateTestCase(Long testCaseId, TestCaseRequest request) {
        TestCase testCase = getTestCaseOrThrow(testCaseId);
        Question question = getQuestionOrThrow(testCase.getQuestionId());
        checkOwnerOrAdmin(question);
        testCase.setInput(request.getInput());
        testCase.setExpectedOutput(request.getExpectedOutput());
        if (request.getIsSample() != null) {
            testCase.setIsSample(request.getIsSample());
        }
        testCaseMapper.update(testCase);
        return toTestCaseVO(testCase);
    }

    @Transactional
    public void deleteTestCase(Long testCaseId) {
        TestCase testCase = getTestCaseOrThrow(testCaseId);
        Question question = getQuestionOrThrow(testCase.getQuestionId());
        checkOwnerOrAdmin(question);
        testCaseMapper.deleteById(testCaseId);
    }

    private Question getQuestionOrThrow(Long id) {
        Question question = questionMapper.findById(id);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }
        return question;
    }

    private TestCase getTestCaseOrThrow(Long id) {
        TestCase testCase = testCaseMapper.findById(id);
        if (testCase == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "测试用例不存在");
        }
        return testCase;
    }

    private boolean canManage(Question question) {
        AuthUser user = AuthContextHolder.get();
        if (user == null) {
            return false;
        }
        if (user.hasRole(Role.ADMIN)) {
            return true;
        }
        return question.getCreatedBy().equals(user.getId());
    }

    private void checkOwnerOrAdmin(Question question) {
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

    private void applyRequest(Question question, QuestionRequest request) {
        question.setTitle(request.getTitle());
        question.setDescription(request.getDescription());
        question.setDifficulty(request.getDifficulty());
        question.setTimeLimit(request.getTimeLimit() == null ? 1000 : request.getTimeLimit());
        question.setMemoryLimit(request.getMemoryLimit() == null ? 256 : request.getMemoryLimit());
        question.setLanguages(
                request.getLanguages() == null ? "cpp,java,python" : request.getLanguages());
        question.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private QuestionVO toVO(Question question) {
        List<TestCaseVO> samples =
                testCaseMapper.findSamplesByQuestionId(question.getId()).stream()
                        .map(this::toTestCaseVO)
                        .toList();
        return QuestionVO.builder()
                .id(question.getId())
                .title(question.getTitle())
                .description(question.getDescription())
                .difficulty(question.getDifficulty())
                .timeLimit(question.getTimeLimit())
                .memoryLimit(question.getMemoryLimit())
                .languages(splitLanguages(question.getLanguages()))
                .status(question.getStatus())
                .createdBy(question.getCreatedBy())
                .sampleTestCases(samples)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    private List<String> splitLanguages(String languages) {
        if (languages == null || languages.isBlank()) {
            return List.of();
        }
        return Arrays.stream(languages.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private TestCaseVO toTestCaseVO(TestCase testCase) {
        return TestCaseVO.builder()
                .id(testCase.getId())
                .input(testCase.getInput())
                .expectedOutput(testCase.getExpectedOutput())
                .isSample(testCase.getIsSample())
                .build();
    }
}
