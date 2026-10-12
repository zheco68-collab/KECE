package com.kece.database.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/** 数据库题目创建/更新请求。 */
@Data
public class DbQuestionRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "题目描述不能为空")
    private String description;

    private String dialect = "MYSQL";

    @NotBlank(message = "题型不能为空")
    private String type;

    /** 建表语句，该题所有测试点共用。 */
    @NotBlank(message = "建表语句不能为空")
    private String schemaSql;

    @NotBlank(message = "标准答案 SQL 不能为空")
    private String referenceSql;

    /** DML 题判分用的校验查询；SELECT 题留空。 */
    private String verifySql;

    private boolean orderSensitive;

    private Integer status = 1;

    @NotEmpty(message = "至少需要一个测试点")
    @Valid
    private List<DbTestCaseRequest> testCases;
}
