package com.kece.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 修改用户状态请求。 */
@Data
public class UpdateStatusRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
