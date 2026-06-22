-- 目标-学生多对多分配表
-- 区分于 goal_assignments(目标-作业),此处存储"目标分给哪些学生"
CREATE TABLE IF NOT EXISTS goal_assignees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ga_goal_student (goal_id, student_id),
    INDEX idx_ga_student (student_id),
    INDEX idx_ga_goal (goal_id),
    CONSTRAINT fk_ga_goal FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE,
    CONSTRAINT fk_ga_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
);
