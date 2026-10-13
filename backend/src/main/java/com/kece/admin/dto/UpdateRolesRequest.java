package com.kece.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/** 修改用户角色请求。 */
@Data
public class UpdateRolesRequest {

    @NotEmpty(message = "角色不能为空")
    private List<String> roles;
}
