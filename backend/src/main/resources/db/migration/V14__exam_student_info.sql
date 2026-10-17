-- KECE 考试考生实名信息与试卷抬头扩展 - Schema v14

-- 1) exam 表增加是否需要填写学生信息配置及学期抬头
ALTER TABLE `exam`
    ADD COLUMN `require_student_info` TINYINT NOT NULL DEFAULT 0 COMMENT '进入考试是否需要填写学生信息 1是 0否' AFTER `status`,
    ADD COLUMN `school_name` VARCHAR(128) DEFAULT '湖北科技职业学院' COMMENT '学校/所属机构名称' AFTER `require_student_info`,
    ADD COLUMN `semester` VARCHAR(128) DEFAULT '2025 - 2026学年度第一学期期末考试' COMMENT '学期/考核类别描述' AFTER `school_name`;

-- 2) 考试考生实名信息登记表
CREATE TABLE IF NOT EXISTS `exam_student_info` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `exam_id`     BIGINT NOT NULL COMMENT '考试ID',
    `user_id`     BIGINT NOT NULL COMMENT '考生用户ID',
    `real_name`   VARCHAR(64) NOT NULL COMMENT '考生姓名',
    `student_no`  VARCHAR(64) NOT NULL COMMENT '考生学号/准考证号',
    `college`     VARCHAR(128) DEFAULT NULL COMMENT '所属学院',
    `major`       VARCHAR(128) DEFAULT NULL COMMENT '年级/专业',
    `class_name`  VARCHAR(128) DEFAULT NULL COMMENT '行政班级',
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exam_user` (`exam_id`, `user_id`),
    KEY `idx_exam_id` (`exam_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试考生实名信息表';
