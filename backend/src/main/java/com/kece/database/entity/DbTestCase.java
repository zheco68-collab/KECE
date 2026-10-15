package com.kece.database.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** 数据库测试点实体，映射 {@code db_test_case} 表。一个测试点只描述一套初始数据。 */
@Data
public class DbTestCase {

    private Long id;
    private Long questionId;
    private Integer caseOrder;

    /** 该测试点的初始数据（INSERT 等）。 */
    private String seedSql;

    /** 期望输出结果集（JSON 或文本），用于样例公开与管理员排查。 */
    private String expectedOutput;

    /** 是否为样例测试点（数据对用户可见）：1是 0否。 */
    private Integer isSample;

    private LocalDateTime createdAt;
}
