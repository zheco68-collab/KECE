-- KECE 考试模块去场次化 - Schema v9
--
-- 去掉 exam_session 中间层：exam 本身就是一次考试，直接承载考试时间与状态。
-- exam_submission.session_id 改名为 exam_id，直接关联 exam。
-- exam 表新增 session_status 字段承载场次状态机（UPCOMING/ONGOING/ENDED/CANCELLED）。
-- 历史数据：exam_session 表无关键业务数据，直接删除；exam_submission 的 session_id
--   若有存量需先迁移到 exam_id（当前无存量考试数据，直接改列名）。

-- 1) exam 表新增考试状态字段
ALTER TABLE `exam`
    ADD COLUMN `session_status` VARCHAR(16) NOT NULL DEFAULT 'UPCOMING'
    COMMENT '考试状态：UPCOMING/ONGOING/ENDED/CANCELLED' AFTER `status`;

-- 2) exam_submission：session_id → exam_id
ALTER TABLE `exam_submission`
    CHANGE COLUMN `session_id` `exam_id` BIGINT NOT NULL COMMENT '考试ID';

-- 重建索引与唯一键（旧键名含 session，替换为 exam）
ALTER TABLE `exam_submission` DROP INDEX `idx_session_user`;
ALTER TABLE `exam_submission` ADD INDEX `idx_exam_user` (`exam_id`, `user_id`);

ALTER TABLE `exam_submission` DROP INDEX `uq_session_user_question`;
ALTER TABLE `exam_submission`
    ADD UNIQUE KEY `uq_exam_user_question` (`exam_id`, `user_id`, `exam_question_id`);

-- 3) 删除 exam_session 表（无外键依赖，直接删除）
DROP TABLE IF EXISTS `exam_session`;
