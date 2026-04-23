-- Learning Goal table schema from learning_goal.sql
-- This schema is applied in the current Spring Boot application database.

CREATE TABLE IF NOT EXISTS goals (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  title VARCHAR(200) NOT NULL COMMENT '目标名称',
  description TEXT COMMENT '目标描述',
  status ENUM('TODO','IN_PROGRESS','DONE','LATE') NOT NULL DEFAULT 'TODO' COMMENT '状态',
  planned_start DATE COMMENT '预计开始日期',
  planned_end DATE COMMENT '预计完成日期',
  actual_start DATE COMMENT '实际开始时间',
  actual_end DATE COMMENT '实际完成时间',
  progress TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '完成进度 0-100',
  owners VARCHAR(500) COMMENT '实施者（逗号分隔）',
  parent_id BIGINT COMMENT '父目标 ID，NULL 表示本身是父目标',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  CONSTRAINT fk_goals_parent FOREIGN KEY (parent_id) REFERENCES goals (id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  INDEX idx_parent_id (parent_id),
  INDEX idx_status (status),
  INDEX idx_planned_end (planned_end),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习目标表（自关联，支持父子层级）';
