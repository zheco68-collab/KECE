-- KECE 数据库 OJ 模块 - Schema v3
--
-- 模型贴合 SQL 题本身：跨测试点不变的东西（建表语句、标准答案、DML 校验、行序要求）
-- 放在题目上，测试点只描述「一套数据」，避免照搬算法题的 input/expected 用例结构。

-- 数据库题目表
CREATE TABLE IF NOT EXISTS `db_question` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    `title`           VARCHAR(255) NOT NULL COMMENT '题目标题',
    `description`     TEXT         NOT NULL COMMENT '题目描述（Markdown）',
    `dialect`         VARCHAR(16)  NOT NULL DEFAULT 'MYSQL' COMMENT 'SQL 方言：MYSQL',
    `type`            VARCHAR(16)  NOT NULL COMMENT '题型：SELECT/DML',
    `schema_sql`      MEDIUMTEXT   DEFAULT NULL COMMENT '建表语句（可多条，分号分隔），所有测试点共用',
    `reference_sql`   MEDIUMTEXT   NOT NULL COMMENT '标准答案：SELECT 题为正确查询，DML 题为正确增删改',
    `verify_sql`      MEDIUMTEXT   DEFAULT NULL COMMENT 'DML 题判分用的校验查询；SELECT 题为 NULL',
    `order_sensitive` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否强制行序比对',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1公开 0私有',
    `created_by`      BIGINT       NOT NULL COMMENT '创建者用户ID',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='数据库题目表';

-- 数据库测试点表：一题多套数据，每题共用题目的建表语句与标准答案
CREATE TABLE IF NOT EXISTS `db_test_case` (
    `id`          BIGINT     NOT NULL AUTO_INCREMENT COMMENT '用例ID',
    `question_id` BIGINT     NOT NULL COMMENT '题目ID',
    `case_order`  INT        NOT NULL DEFAULT 0 COMMENT '用例顺序',
    `seed_sql`    MEDIUMTEXT DEFAULT NULL COMMENT '该测试点的初始数据（INSERT 等）',
    `is_sample`   TINYINT    NOT NULL DEFAULT 0 COMMENT '是否为样例测试点（数据对用户可见）',
    `created_at`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_question_id` (`question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='数据库测试点表';

-- 数据库提交记录表（独立于代码 submission 表）
CREATE TABLE IF NOT EXISTS `db_submission` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '提交ID',
    `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
    `question_id`  BIGINT       NOT NULL COMMENT '题目ID',
    `dialect`      VARCHAR(16)  NOT NULL DEFAULT 'MYSQL' COMMENT 'SQL 方言',
    `sql_content`  TEXT         NOT NULL COMMENT '提交的 SQL（单条语句）',
    `status`       VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '评测状态',
    `score`        INT          DEFAULT NULL COMMENT '得分（满分 100）',
    `time_used_ms` INT          DEFAULT NULL COMMENT '评测耗时',
    `result_json`  MEDIUMTEXT   DEFAULT NULL COMMENT '评测结果详情（JSON）',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_question_id` (`question_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='数据库提交记录表';
