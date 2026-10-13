package com.kece.admin.controller;

import com.kece.admin.dto.RejudgeResultVO;
import com.kece.common.response.ApiResponse;
import com.kece.common.response.PageResult;
import com.kece.common.security.RequireRole;
import com.kece.common.security.Role;
import com.kece.database.dto.DbSubmissionListVO;
import com.kece.database.service.DbSubmissionService;
import com.kece.objective.dto.ObjectiveSubmissionListVO;
import com.kece.objective.service.ObjectiveQuestionService;
import com.kece.submission.dto.SubmissionListVO;
import com.kece.submission.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台提交监控接口：三类提交的跨用户列表（白名单过滤在 Service 层强制）。 */
@Tag(name = "后台管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSubmissionController {

    private final SubmissionService submissionService;
    private final DbSubmissionService dbSubmissionService;
    private final ObjectiveQuestionService objectiveQuestionService;

    @Operation(summary = "代码题提交列表（管理）")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @GetMapping("/submissions")
    public ApiResponse<PageResult<SubmissionListVO>> listCodeSubmissions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long questionId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                submissionService.listAdmin(status, userId, questionId, page, size));
    }

    @Operation(summary = "数据库题提交列表（管理）")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @GetMapping("/db-submissions")
    public ApiResponse<PageResult<DbSubmissionListVO>> listDbSubmissions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long questionId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                dbSubmissionService.listAdmin(status, userId, questionId, page, size));
    }

    @Operation(summary = "客观题作答列表（管理）")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @GetMapping("/objective-submissions")
    public ApiResponse<PageResult<ObjectiveSubmissionListVO>> listObjectiveSubmissions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long questionId,
            @RequestParam(required = false) Integer correct,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                objectiveQuestionService.listSubmissionsAdmin(
                        correct, userId, questionId, page, size));
    }

    @Operation(summary = "代码题提交重判（单条）")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @PostMapping("/submissions/{id}/rejudge")
    public ApiResponse<RejudgeResultVO> rejudgeSubmission(@PathVariable Long id) {
        return ApiResponse.success(submissionService.rejudge(id));
    }

    @Operation(summary = "数据库题提交重判（单条）")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @PostMapping("/db-submissions/{id}/rejudge")
    public ApiResponse<RejudgeResultVO> rejudgeDbSubmission(@PathVariable Long id) {
        return ApiResponse.success(dbSubmissionService.rejudge(id));
    }

    @Operation(summary = "客观题作答重判（单条）")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @PostMapping("/objective-submissions/{id}/rejudge")
    public ApiResponse<RejudgeResultVO> rejudgeObjectiveSubmission(@PathVariable Long id) {
        return ApiResponse.success(objectiveQuestionService.rejudge(id));
    }

    @Operation(summary = "客观题按题批量重判")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    @PostMapping("/objective-questions/{id}/rejudge")
    public ApiResponse<RejudgeResultVO> rejudgeObjectiveQuestion(@PathVariable Long id) {
        return ApiResponse.success(objectiveQuestionService.rejudgeByQuestion(id));
    }
}
