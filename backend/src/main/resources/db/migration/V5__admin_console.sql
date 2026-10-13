-- KECE 后台管理模块 - Schema v5
--
-- 1) `objective_question` 补 `status`：三类题目表统一「1公开 0私有」两态语义。
--    DEFAULT 1 使存量题目全部保持公开，与现状（无过滤、人人可见）一致，无回归。
-- 2) `objective_submission` 补 `question_id` 单列索引：
--    删除保护要按 `question_id` 做 EXISTS 检查，现有索引 (`user_id`, `question_id`)
--    前缀是 `user_id`，单查 `question_id` 用不上，会全表扫。

ALTER TABLE `objective_question`
    ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1公开 0私有' AFTER `created_by`,
    ADD KEY `idx_status` (`status`);

ALTER TABLE `objective_submission`
    ADD KEY `idx_question_id` (`question_id`);
