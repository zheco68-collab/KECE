-- 提交评测结果详情持久化：判题机将终态结果写入 Redis 后，后端回写落库。
ALTER TABLE `submission`
    ADD COLUMN `result_json` MEDIUMTEXT DEFAULT NULL COMMENT '评测结果详情(JSON，含逐用例报告)' AFTER `error_log`;
