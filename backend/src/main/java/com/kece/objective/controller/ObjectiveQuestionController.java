package com.kece.objective.controller;

import com.kece.common.response.ApiResponse;
import com.kece.common.response.PageResult;
import com.kece.common.security.RequireRole;
import com.kece.common.security.Role;
import com.kece.objective.dto.AnswerResultVO;
import com.kece.objective.dto.AnswerSubmitRequest;
import com.kece.objective.dto.ObjectiveQuestionRequest;
import com.kece.objective.dto.ObjectiveQuestionVO;
import com.kece.objective.service.ObjectiveQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/** 客观题接口。 */
@Tag(name = "客观题")
@RestController
@RequestMapping("/objective-questions")
@RequiredArgsConstructor
public class ObjectiveQuestionController {

    private final ObjectiveQuestionService service;

    @Operation(summary = "客观题列表（分页/筛选）")
    @GetMapping
    public ApiResponse<PageResult<ObjectiveQuestionVO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.list(type, keyword, page, size));
    }

    @Operation(summary = "客观题详情")
    @GetMapping("/{id}")
    public ApiResponse<ObjectiveQuestionVO> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id));
    }

    @Operation(summary = "创建客观题")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @PostMapping
    public ApiResponse<ObjectiveQuestionVO> create(
            @Valid @RequestBody ObjectiveQuestionRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @Operation(summary = "更新客观题")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @PutMapping("/{id}")
    public ApiResponse<ObjectiveQuestionVO> update(
            @PathVariable Long id, @Valid @RequestBody ObjectiveQuestionRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @Operation(summary = "删除客观题")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "客观题作答并即时判分")
    @RequireRole
    @PostMapping("/{id}/submit")
    public ApiResponse<AnswerResultVO> submit(
            @PathVariable Long id, @Valid @RequestBody AnswerSubmitRequest request) {
        return ApiResponse.success(service.submit(id, request));
    }
}
