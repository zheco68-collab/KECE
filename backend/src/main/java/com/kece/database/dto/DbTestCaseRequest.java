package com.kece.database.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 数据库测试点创建/更新请求：只描述一套初始数据，表结构与标准答案在题目上。 */
@Data
public class DbTestCaseRequest {

    /** 该测试点的初始数据（INSERT 等）。 */
    @NotBlank(message = "测试点数据不能为空")
    private String seedSql;

    /** 0 非样例 / 1 样例，与代码题 TestCaseRequest 保持一致。 */
    private Integer isSample = 0;
}
