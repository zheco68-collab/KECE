package com.kece.database.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** 数据库题目实体，映射 {@code db_question} 表。 */
@Data
public class DbQuestion {

    private Long id;

    private String title;

    /** 题目描述（Markdown）。 */
    private String description;

    /** SQL 方言：MYSQL。 */
    private String dialect;

    /** 题型：SELECT/DML。 */
    private String type;

    /** 建表语句（可多条，分号分隔），该题所有测试点共用。 */
    private String schemaSql;

    /** 标准答案：SELECT 题为正确查询，DML 题为正确增删改。 */
    private String referenceSql;

    /** DML 题判分用的校验查询；SELECT 题为 NULL。 */
    private String verifySql;

    /** 是否强制行序比对：1是 0否。 */
    private Integer orderSensitive;

    /** 状态：1公开 0私有。 */
    private Integer status;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
