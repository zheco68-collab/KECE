ALTER TABLE `db_test_case`
    ADD COLUMN `expected_output` MEDIUMTEXT DEFAULT NULL COMMENT '样例期望输出结果集（JSON/文本）' AFTER `seed_sql`;

