-- KECE 数据库题复用考试评测链路 - Schema v8
--
-- db_submission 缺 contest_id：考试数据库题交卷后写 db_submission 复用判题机与回写链路，
-- 排名/成绩需按场次关联。纯新增列，无回填负担。

ALTER TABLE `db_submission`
    ADD COLUMN `contest_id` BIGINT DEFAULT NULL COMMENT '考试场次ID（复用评测链路关联）' AFTER `question_id`,
    ADD KEY `idx_contest_id` (`contest_id`);
