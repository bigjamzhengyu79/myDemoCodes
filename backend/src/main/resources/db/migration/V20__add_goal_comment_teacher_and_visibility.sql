-- 目标评论表改造：支持老师评论、公开/私密两种可见性
-- 说明：Hibernate ddl-auto=update 可能已自动添加 author_id 等字段，
--       请逐条执行，跳过已存在的列

-- ====== 第一步：修改 student_id 为可空 ======
ALTER TABLE goal_comments MODIFY COLUMN student_id BIGINT NULL;

-- ====== 第二步：添加新列（如提示 Duplicate column 说明已存在，跳过即可） ======
ALTER TABLE goal_comments ADD COLUMN author_id BIGINT NULL AFTER student_id;
ALTER TABLE goal_comments ADD COLUMN author_role VARCHAR(10) NOT NULL DEFAULT 'STUDENT' AFTER author_id;
ALTER TABLE goal_comments ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC' AFTER author_role;
ALTER TABLE goal_comments ADD COLUMN target_student_id BIGINT NULL AFTER visibility;

-- ====== 第三步：迁移已有历史数据 ======
UPDATE goal_comments SET author_id = student_id WHERE author_id IS NULL;

-- ====== 第四步：author_id 设为 NOT NULL ======
ALTER TABLE goal_comments MODIFY COLUMN author_id BIGINT NOT NULL;

-- ====== 第五步：添加索引（如提示 Duplicate key name 说明已存在，跳过即可） ======
ALTER TABLE goal_comments ADD INDEX idx_gc_author (author_id);
ALTER TABLE goal_comments ADD INDEX idx_gc_visibility (visibility);
ALTER TABLE goal_comments ADD INDEX idx_gc_target (target_student_id);

-- ====== 第六步：外键（先删除旧的 student 外键，再添加 author 外键） ======
-- 注意：MySQL 不支持 DROP FOREIGN KEY IF EXISTS，请先查看现有外键名
-- SHOW CREATE TABLE goal_comments;
-- 假设旧外键名为 fk_gc_student，如果不存在这条命令会报错，可忽略
ALTER TABLE goal_comments DROP FOREIGN KEY fk_gc_student;
ALTER TABLE goal_comments ADD CONSTRAINT fk_gc_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE;