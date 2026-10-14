-- KECE 个人中心与用户资料扩展
ALTER TABLE `user`
    ADD COLUMN `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像图片访问URL' AFTER `email`,
    ADD COLUMN `bio` VARCHAR(255) DEFAULT NULL COMMENT '个人简介/Slogan' AFTER `avatar_url`,
    ADD COLUMN `github_url` VARCHAR(255) DEFAULT NULL COMMENT 'GitHub 主页链接' AFTER `bio`;

