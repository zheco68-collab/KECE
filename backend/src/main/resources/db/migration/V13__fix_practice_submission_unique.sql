-- KECE 课练提交记录唯一约束 - Schema v13
-- 清理历史由于缺乏约束可能产生的重复记录（保留相同 课练+用户+题目 中分数最高且ID最新的一条）
DELETE t1 FROM `lesson_practice_submission` t1
INNER JOIN `lesson_practice_submission` t2 
WHERE t1.practice_id = t2.practice_id 
  AND t1.user_id = t2.user_id 
  AND t1.practice_question_id = t2.practice_question_id 
  AND (t1.score < t2.score OR (t1.score = t2.score AND t1.id < t2.id));

-- 添加唯一键约束，确保课练中一个用户对一题仅保留一条最终记录，防止并发提交引发 Duplicate/TooManyResultsException
ALTER TABLE `lesson_practice_submission`
    ADD UNIQUE KEY `uq_practice_user_question` (`practice_id`, `user_id`, `practice_question_id`);
