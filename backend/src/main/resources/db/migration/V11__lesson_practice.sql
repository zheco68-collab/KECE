-- KECE 课练模块 - Schema v11
--
-- 1) lesson_practice：课练房间表
-- 2) lesson_practice_question：课练题目快照表（支持 OBJECTIVE, ALGO, DB 三种题型）
-- 3) lesson_practice_member：课练成员表（参与的学生）
-- 4) lesson_practice_submission：课练提交记录表

CREATE TABLE IF NOT EXISTS `lesson_practice` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '课练ID',
    `title`            VARCHAR(255) NOT NULL COMMENT '课练标题',
    `description`      TEXT         DEFAULT NULL COMMENT '课练说明',
    `access_code`      VARCHAR(8)   NOT NULL COMMENT '6位大写字母数字房间口令',
    `duration_minutes` INT          NOT NULL DEFAULT 45 COMMENT '课练时长（分钟，0表示不限时）',
    `status`           VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/ENDED',
    `teacher_id`       BIGINT       NOT NULL COMMENT '发起教师用户ID',
    `start_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    `end_time`         DATETIME     DEFAULT NULL COMMENT '结束时间',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_access_code` (`access_code`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='课练房间表';

CREATE TABLE IF NOT EXISTS `lesson_practice_question` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目条目ID',
    `practice_id`    BIGINT       NOT NULL COMMENT '课练ID',
    `question_id`    BIGINT       NOT NULL COMMENT '题目原ID（对应 objective_question/question/db_question）',
    `question_type`  VARCHAR(16)  NOT NULL COMMENT '题型：OBJECTIVE/ALGO/DB',
    `title`          VARCHAR(255) NOT NULL COMMENT '题目标题快照',
    `score`          INT          NOT NULL DEFAULT 10 COMMENT '分值',
    `sort_order`     INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_practice_id` (`practice_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='课练题目表';

CREATE TABLE IF NOT EXISTS `lesson_practice_member` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '成员记录ID',
    `practice_id`      BIGINT       NOT NULL COMMENT '课练ID',
    `user_id`          BIGINT       NOT NULL COMMENT '学生用户ID',
    `total_score`      INT          NOT NULL DEFAULT 0 COMMENT '当前总得分',
    `solved_count`     INT          NOT NULL DEFAULT 0 COMMENT '已通过题目数',
    `joined_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `submitted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已主动交卷',
    `submitted_at`     DATETIME     DEFAULT NULL COMMENT '交卷时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_practice_user` (`practice_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='课练成员表';

CREATE TABLE IF NOT EXISTS `lesson_practice_submission` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '提交记录ID',
    `practice_id`           BIGINT       NOT NULL COMMENT '课练ID',
    `practice_question_id`  BIGINT       NOT NULL COMMENT '课练题目ID (对应 lesson_practice_question.id)',
    `user_id`               BIGINT       NOT NULL COMMENT '学生用户ID',
    `question_type`         VARCHAR(16)  NOT NULL COMMENT '题型：OBJECTIVE/ALGO/DB',
    `answer`                MEDIUMTEXT   DEFAULT NULL COMMENT '客观题作答答案或代码',
    `submission_id`         BIGINT       DEFAULT NULL COMMENT '关联全局算法/数据库 submission.id',
    `status`                VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：ACCEPTED/WRONG_ANSWER等',
    `score`                 INT          NOT NULL DEFAULT 0 COMMENT '本题得分',
    `result_json`           MEDIUMTEXT   DEFAULT NULL COMMENT '判题详情',
    `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_practice_user` (`practice_id`, `user_id`),
    KEY `idx_practice_question` (`practice_question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='课练提交表';

