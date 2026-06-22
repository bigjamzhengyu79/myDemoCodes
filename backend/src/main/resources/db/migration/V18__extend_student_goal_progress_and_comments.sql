-- 1. 扩展 student_goal_progress 表，增加学生个人实际时间
ALTER TABLE student_goal_progress 
  ADD COLUMN actual_start DATE NULL AFTER status,
  ADD COLUMN actual_end DATE NULL AFTER actual_start;

-- 2. 新建目标评论表（每个学生对每个目标节点独立评论，学生之间互不可见）
CREATE TABLE IF NOT EXISTS goal_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    content TEXT NOT NULL COMMENT '评论内容，支持LaTeX富文本',
    image_urls TEXT NULL COMMENT '图片URL列表，JSON数组格式',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_gc_goal (goal_id),
    INDEX idx_gc_student (student_id),
    CONSTRAINT fk_gc_goal FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE,
    CONSTRAINT fk_gc_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
);