package com.kece.admin.controller;

import com.kece.admin.dto.AdminUserVO;
import com.kece.admin.dto.ResetPasswordRequest;
import com.kece.admin.dto.UpdateRolesRequest;
import com.kece.admin.dto.UpdateStatusRequest;
import com.kece.admin.service.AdminService;
import com.kece.common.response.ApiResponse;
import com.kece.common.response.PageResult;
import com.kece.common.security.RequireRole;
import com.kece.common.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台用户管理接口。 */
@Tag(name = "后台管理")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;

    @Operation(summary = "用户列表（分页/筛选）")
    @RequireRole(Role.ADMIN)
    @GetMapping
    public ApiResponse<PageResult<AdminUserVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.listUsers(keyword, role, status, page, size));
    }

    @Operation(summary = "修改用户角色")
    @RequireRole(Role.ADMIN)
    @PutMapping("/{id}/roles")
    public ApiResponse<AdminUserVO> updateRoles(
            @PathVariable Long id, @Valid @RequestBody UpdateRolesRequest request) {
        return ApiResponse.success(adminService.updateRoles(id, request));
    }

    @Operation(summary = "修改用户状态")
    @RequireRole(Role.ADMIN)
    @PutMapping("/{id}/status")
    public ApiResponse<AdminUserVO> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return ApiResponse.success(adminService.updateStatus(id, request));
    }

    @Operation(summary = "重置用户密码")
    @RequireRole(Role.ADMIN)
    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(
            @PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        adminService.resetPassword(id, request);
        return ApiResponse.success();
    }
}
