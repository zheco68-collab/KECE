-- KECE 考试作答 UPSERT 与交卷标记 - Schema v7
--
-- exam_submission 按「场次 + 考生 + 题目快照条目」幂等 UPSERT（保存作答可重复提交，覆盖旧答案）。
-- 新增 language（代码题评测所需）与 submitted（交卷幂等标记）两列。新建表至今无存量数据，无回填负担。

ALTER TABLE `exam_submission`
    ADD COLUMN `language` VARCHAR(32) DEFAULT NULL COMMENT '代码题语言（cpp/java/python），其余题型为空' AFTER `answer_content`,
    ADD COLUMN `submitted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已随交卷评测：1是 0否' AFTER `status`,
    ADD UNIQUE KEY `uq_session_user_question` (`session_id`, `user_id`, `exam_question_id`);
