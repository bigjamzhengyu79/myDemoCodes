-- goals 表增加 class_group_id 外键（目标关联班级）
ALTER TABLE goals ADD COLUMN class_group_id BIGINT;
ALTER TABLE goals ADD CONSTRAINT fk_goal_class_group
    FOREIGN KEY (class_group_id) REFERENCES class_groups(id) ON DELETE SET NULL;
CREATE INDEX idx_goal_class_group ON goals(class_group_id);