-- KECE 综合编程与技能评测平台 - 数据库 Schema v1

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`      VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码哈希',
    `email`         VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `roles`         VARCHAR(255) NOT NULL DEFAULT 'USER' COMMENT '角色列表，逗号分隔（USER/TEACHER/ADMIN）',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- 算法题目表
CREATE TABLE IF NOT EXISTS `question` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    `title`        VARCHAR(255) NOT NULL COMMENT '题目标题',
    `description`  TEXT         NOT NULL COMMENT '题目描述（Markdown）',
    `difficulty`   VARCHAR(16)  NOT NULL DEFAULT 'EASY' COMMENT '难度：EASY/MEDIUM/HARD',
    `time_limit`   INT          NOT NULL DEFAULT 1000 COMMENT '时间限制（ms）',
    `memory_limit` INT          NOT NULL DEFAULT 256 COMMENT '内存限制（MB）',
    `languages`    VARCHAR(255) NOT NULL DEFAULT 'cpp,java,python' COMMENT '支持的语言，逗号分隔',
    `created_by`   BIGINT       NOT NULL COMMENT '创建者用户ID',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1公开 0私有',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_created_by` (`created_by`),
    KEY `idx_difficulty` (`difficulty`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='算法题目表';

-- 测试用例表
CREATE TABLE IF NOT EXISTS `test_case` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '用例ID',
    `question_id`    BIGINT   NOT NULL COMMENT '题目ID',
    `input`          MEDIUMTEXT DEFAULT NULL COMMENT '输入数据',
    `expected_output` MEDIUMTEXT NOT NULL COMMENT '期望输出',
    `is_sample`      TINYINT  NOT NULL DEFAULT 0 COMMENT '是否为样例用例（对用户可见）',
    `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_question_id` (`question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='测试用例表';

-- 客观题表
CREATE TABLE IF NOT EXISTS `objective_question` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    `type`       VARCHAR(16)  NOT NULL COMMENT '题型：SINGLE/MULTIPLE/FILL',
    `content`    TEXT         NOT NULL COMMENT '题干（Markdown，支持代码高亮和LaTeX）',
    `options`    JSON         DEFAULT NULL COMMENT '选项（单选/多选题）：["A.xxx","B.xxx"]',
    `answer`     VARCHAR(512) NOT NULL COMMENT '答案：单选"A"；多选"A,C"；填空"keyword"或正则',
    `match_type` VARCHAR(16)  NOT NULL DEFAULT 'EXACT' COMMENT '填空匹配方式：EXACT/IGNORE_CASE/REGEX',
    `score`      INT          NOT NULL DEFAULT 5 COMMENT '分值',
    `created_by` BIGINT       NOT NULL COMMENT '创建者用户ID',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='客观题表';

-- 提交记录表
CREATE TABLE IF NOT EXISTS `submission` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '提交ID',
    `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
    `question_id`    BIGINT       NOT NULL COMMENT '题目ID',
    `contest_id`     BIGINT       DEFAULT NULL COMMENT '比赛/考试ID（选填）',
    `language`       VARCHAR(32)  NOT NULL COMMENT '语言：cpp/java/python/sql/frontend_react 等',
    `code_content`   MEDIUMTEXT   NOT NULL COMMENT '提交的代码内容（单文件或多文件JSON）',
    `status`         VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '评测状态',
    `score`          INT          DEFAULT NULL COMMENT '得分（OI模式）',
    `time_used_ms`   INT          DEFAULT NULL COMMENT '最大运行耗时',
    `memory_used_kb` INT          DEFAULT NULL COMMENT '最大内存消耗',
    `error_log`      TEXT         DEFAULT NULL COMMENT '编译/运行错误日志',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_question_id` (`question_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='提交记录表';
