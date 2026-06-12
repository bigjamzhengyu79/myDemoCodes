-- 移除 users 表中的 class_name 列（已迁移到 class_group_students 关联表）
ALTER TABLE users DROP COLUMN class_name;