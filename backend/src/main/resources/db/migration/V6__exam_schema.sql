-- KECE 组卷考试模块 - Schema v6
--
-- 1) exam：试卷（组卷的静态配置，题目以快照条目关联）
-- 2) exam_question：试卷与题目的关联 + 题目快照（标题/分值在组卷时定格，后续改题不影响已组卷）
-- 3) exam_session：考试场次（某试卷的一次考试实例，含时间窗口与时长）
-- 4) exam_submission：考试提交（考生在某场次对某题快照条目的作答，结果落快照行）
--
-- 沿用全库 0 外键的既定设计：引用关系由应用层维护，删除保护同理。

-- 试卷表
CREATE TABLE IF NOT EXISTS `exam` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '试卷ID',
    `title`            VARCHAR(255) NOT NULL COMMENT '试卷标题',
    `description`      TEXT         DEFAULT NULL COMMENT '试卷说明（Markdown）',
    `duration_minutes` INT          NOT NULL DEFAULT 120 COMMENT '考试时长（分钟）',
    `start_time`       DATETIME     DEFAULT NULL COMMENT '开考时间窗口开始（NULL 表示不限制）',
    `end_time`         DATETIME     DEFAULT NULL COMMENT '最晚开考时间（NULL 表示不限制）',
    `mode`             VARCHAR(16)  NOT NULL DEFAULT 'OI' COMMENT '评分模式：ACM/OI',
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1公开 0私有',
    `created_by`       BIGINT       NOT NULL COMMENT '创建者用户ID',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_created_by` (`created_by`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='试卷表';

-- 试卷题目快照表
CREATE TABLE IF NOT EXISTS `exam_question` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '试卷题目条目ID',
    `exam_id`        BIGINT       NOT NULL COMMENT '试卷ID',
    `question_id`    BIGINT       NOT NULL COMMENT '原题ID（引用；评测时取题目当前数据）',
    `question_type`  VARCHAR(16)  NOT NULL COMMENT '题型：ALGO/DB/OBJECTIVE',
    `title`          VARCHAR(255) NOT NULL COMMENT '题目标题快照（组卷时定格）',
    `score`          INT          NOT NULL DEFAULT 5 COMMENT '本题分值快照（组卷时定格）',
    `sort_order`     INT          NOT NULL DEFAULT 0 COMMENT '组卷顺序（升序）',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_exam_id` (`exam_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='试卷题目快照表';

-- 考试场次表
CREATE TABLE IF NOT EXISTS `exam_session` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '场次ID',
    `exam_id`          BIGINT       NOT NULL COMMENT '试卷ID',
    `start_time`       DATETIME     NOT NULL COMMENT '本场开考时间',
    `end_time`         DATETIME     NOT NULL COMMENT '本场结束时间（start + duration）',
    `duration_minutes` INT          NOT NULL DEFAULT 120 COMMENT '本场时长（分钟）',
    `status`           VARCHAR(16)  NOT NULL DEFAULT 'UPCOMING' COMMENT '状态：UPCOMING/ONGOING/ENDED/CANCELLED',
    `created_by`       BIGINT       NOT NULL COMMENT '创建者用户ID',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_exam_id` (`exam_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='考试场次表';

-- 考试提交表
CREATE TABLE IF NOT EXISTS `exam_submission` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '考试提交ID',
    `session_id`       BIGINT       NOT NULL COMMENT '场次ID',
    `user_id`          BIGINT       NOT NULL COMMENT '考生用户ID',
    `exam_question_id` BIGINT       NOT NULL COMMENT '试卷题目快照条目ID',
    `answer_content`   MEDIUMTEXT   NOT NULL COMMENT '作答内容（代码多文件JSON / SQL原文 / 客观题答案）',
    `status`           VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '评测状态（客观题同步判分，代码/数据库题走队列）',
    `score`            INT          DEFAULT NULL COMMENT '得分（OI 模式/客观题）',
    `result_json`      MEDIUMTEXT   DEFAULT NULL COMMENT '评测详情 JSON',
    `submitted_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_session_user` (`session_id`, `user_id`),
    KEY `idx_exam_question` (`exam_question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='考试提交表';
