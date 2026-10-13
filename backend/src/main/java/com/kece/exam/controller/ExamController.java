package com.kece.exam.controller;

import com.kece.common.response.ApiResponse;
import com.kece.common.response.PageResult;
import com.kece.common.security.RequireRole;
import com.kece.common.security.Role;
import com.kece.exam.dto.ExamRequest;
import com.kece.exam.dto.ExamVO;
import com.kece.exam.service.ExamService;
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

/** 考试与组卷接口（CRUD + 组卷）。 */
@Tag(name = "考试管理")
@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @Operation(summary = "考试列表（分页/筛选）")
    @GetMapping
    public ApiResponse<PageResult<ExamVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean all) {
        return ApiResponse.success(examService.list(keyword, status, page, size, all));
    }

    @Operation(summary = "后台考试列表（分页/筛选，非 ADMIN 仅见自己的）")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @GetMapping("/admin")
    public ApiResponse<PageResult<ExamVO>> listAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(examService.listAdmin(keyword, status, createdBy, page, size));
    }

    @Operation(summary = "考试详情（含题目快照）")
    @GetMapping("/{id}")
    public ApiResponse<ExamVO> detail(@PathVariable Long id) {
        return ApiResponse.success(examService.detail(id));
    }

    @Operation(summary = "创建考试（组卷并定格题目快照）")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @PostMapping
    public ApiResponse<ExamVO> create(@Valid @RequestBody ExamRequest request) {
        return ApiResponse.success(examService.create(request));
    }

    @Operation(summary = "更新考试（替换题目快照，已有作答后不可改题）")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @PutMapping("/{id}")
    public ApiResponse<ExamVO> update(
            @PathVariable Long id, @Valid @RequestBody ExamRequest request) {
        return ApiResponse.success(examService.update(id, request));
    }

    @Operation(summary = "删除考试（已有作答记录后不可删除）")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        examService.delete(id);
        return ApiResponse.success();
    }
}
