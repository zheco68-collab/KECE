package com.kece.database.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 数据库题目作答请求。 */
@Data
public class DbSubmitRequest {

    @NotBlank(message = "SQL 不能为空")
    private String sql;
}
