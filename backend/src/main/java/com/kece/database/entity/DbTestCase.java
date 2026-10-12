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

    /** 是否为样例测试点（数据对用户可见）：1是 0否。 */
    private Integer isSample;

    private LocalDateTime createdAt;
}
