package com.kece.objective.controller;

import com.kece.common.response.ApiResponse;
import com.kece.common.response.PageResult;
import com.kece.common.security.RequireRole;
import com.kece.objective.dto.ObjectiveSubmissionListVO;
import com.kece.objective.service.ObjectiveQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 客观题作答记录接口（与 /submissions、/db-submissions 并列的第三类提交记录）。 */
@Tag(name = "客观题")
@RestController
@RequestMapping("/objective-submissions")
@RequiredArgsConstructor
public class ObjectiveSubmissionController {

    private final ObjectiveQuestionService service;

    @Operation(summary = "我的客观题作答记录（分页/对错筛选）")
    @RequireRole
    @GetMapping
    public ApiResponse<PageResult<ObjectiveSubmissionListVO>> list(
            @RequestParam(required = false) Integer correct,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.listSubmissions(correct, page, size));
    }

    @Operation(summary = "当前用户已答对的客观题 id 列表")
    @RequireRole
    @GetMapping("/solved")
    public ApiResponse<List<Long>> solved() {
        return ApiResponse.success(service.solvedQuestionIds());
    }
}
