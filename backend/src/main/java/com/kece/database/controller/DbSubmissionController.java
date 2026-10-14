package com.kece.database.controller;

import com.kece.common.response.ApiResponse;
import com.kece.common.response.PageResult;
import com.kece.common.security.RequireRole;
import com.kece.database.dto.DbSubmissionListVO;
import com.kece.database.service.DbSubmissionService;
import com.kece.submission.dto.SubmissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 数据库提交记录接口。 */
@Tag(name = "数据库 OJ")
@RestController
@RequestMapping("/db-submissions")
@RequiredArgsConstructor
public class DbSubmissionController {

    private final DbSubmissionService submissionService;

    @Operation(summary = "数据库提交记录列表（分页/状态筛选/可指定 userId）")
    @RequireRole
    @GetMapping
    public ApiResponse<PageResult<DbSubmissionListVO>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(submissionService.list(userId, status, page, size));
    }

    @Operation(summary = "当前用户已通过的数据库题目 id 列表")
    @RequireRole
    @GetMapping("/solved")
    public ApiResponse<List<Long>> solved() {
        return ApiResponse.success(submissionService.solvedQuestionIds());
    }

    @Operation(summary = "查询数据库评测状态与结果（轮询）")
    @RequireRole
    @GetMapping("/{id}")
    public ApiResponse<SubmissionVO> get(@PathVariable Long id) {
        return ApiResponse.success(submissionService.get(id));
    }
}
