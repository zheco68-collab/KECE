-- KECE 客观题模块 - Schema v4
--
-- 客观题作答记录：客观题判分是同步即时完成的（不走消息队列，也没有判题机参与），
-- 因此单表逐次留痕即可，不需要算法题那种「status + result_json」的结果态结构。
-- 每次作答插入一行；「已答对」= 该用户在该题上存在 correct = 1 的记录。

CREATE TABLE IF NOT EXISTS `objective_submission` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '作答记录ID',
    `user_id`     BIGINT       NOT NULL COMMENT '作答用户ID',
    `question_id` BIGINT       NOT NULL COMMENT '客观题ID',
    `answer`      VARCHAR(512) NOT NULL COMMENT '提交的答案原文',
    `correct`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否完全正确：1是 0否（多选漏选为0）',
    `score`       INT          NOT NULL DEFAULT 0 COMMENT '本次得分',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_question` (`user_id`, `question_id`),
    KEY `idx_user_correct` (`user_id`, `correct`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='客观题作答记录表';
