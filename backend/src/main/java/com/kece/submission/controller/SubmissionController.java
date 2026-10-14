package com.kece.submission.controller;

import com.kece.common.response.ApiResponse;
import com.kece.common.response.PageResult;
import com.kece.common.security.RequireRole;
import com.kece.submission.dto.SubmissionListVO;
import com.kece.submission.dto.SubmissionVO;
import com.kece.submission.dto.SubmitRequest;
import com.kece.submission.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 代码提交与评测接口。 */
@Tag(name = "代码提交与评测")
@RestController
@RequestMapping("/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @Operation(summary = "提交代码（异步评测）")
    @RequireRole
    @PostMapping
    public ApiResponse<SubmissionVO> submit(@Valid @RequestBody SubmitRequest request) {
        return ApiResponse.success(submissionService.submit(request));
    }

    @Operation(summary = "提交记录列表（分页/状态筛选/可指定 userId）")
    @RequireRole
    @GetMapping
    public ApiResponse<PageResult<SubmissionListVO>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(submissionService.list(userId, status, page, size));
    }

    @Operation(summary = "当前用户已通过的题目 id 列表")
    @RequireRole
    @GetMapping("/solved")
    public ApiResponse<List<Long>> solved() {
        return ApiResponse.success(submissionService.solvedQuestionIds());
    }

    @Operation(summary = "查询评测状态与结果（轮询）")
    @RequireRole
    @GetMapping("/{id}")
    public ApiResponse<SubmissionVO> get(@PathVariable Long id) {
        return ApiResponse.success(submissionService.get(id));
    }
}
