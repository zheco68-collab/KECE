package com.kece.question.controller;

import com.kece.common.response.ApiResponse;
import com.kece.common.response.PageResult;
import com.kece.common.security.RequireRole;
import com.kece.common.security.Role;
import com.kece.question.dto.QuestionRequest;
import com.kece.question.dto.QuestionVO;
import com.kece.question.dto.TestCaseRequest;
import com.kece.question.dto.TestCaseVO;
import com.kece.question.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 算法题库接口。 */
@Tag(name = "算法题库")
@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "题库列表（分页/筛选）")
    @GetMapping
    public ApiResponse<PageResult<QuestionVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean all) {
        return ApiResponse.success(questionService.list(keyword, difficulty, page, size, all));
    }

    @Operation(summary = "题目详情")
    @GetMapping("/{id}")
    public ApiResponse<QuestionVO> detail(@PathVariable Long id) {
        return ApiResponse.success(questionService.detail(id));
    }

    @Operation(summary = "创建题目")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @PostMapping
    public ApiResponse<QuestionVO> create(@Valid @RequestBody QuestionRequest request) {
        return ApiResponse.success(questionService.create(request));
    }

    @Operation(summary = "更新题目")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @PutMapping("/{id}")
    public ApiResponse<QuestionVO> update(
            @PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return ApiResponse.success(questionService.update(id, request));
    }

    @Operation(summary = "删除题目")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "题目测试用例列表（出题人/管理员）")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @GetMapping("/{id}/test-cases")
    public ApiResponse<List<TestCaseVO>> listTestCases(@PathVariable Long id) {
        return ApiResponse.success(questionService.listTestCases(id));
    }

    @Operation(summary = "新增测试用例")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @PostMapping("/{id}/test-cases")
    public ApiResponse<TestCaseVO> addTestCase(
            @PathVariable Long id, @Valid @RequestBody TestCaseRequest request) {
        return ApiResponse.success(questionService.addTestCase(id, request));
    }

    @Operation(summary = "更新测试用例")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @PutMapping("/test-cases/{testCaseId}")
    public ApiResponse<TestCaseVO> updateTestCase(
            @PathVariable Long testCaseId, @Valid @RequestBody TestCaseRequest request) {
        return ApiResponse.success(questionService.updateTestCase(testCaseId, request));
    }

    @Operation(summary = "删除测试用例")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @DeleteMapping("/test-cases/{testCaseId}")
    public ApiResponse<Void> deleteTestCase(@PathVariable Long testCaseId) {
        questionService.deleteTestCase(testCaseId);
        return ApiResponse.success();
    }
}
