-- 学生个人目标进度表：每个学生对被分配的目标维护独立进度
CREATE TABLE IF NOT EXISTS student_goal_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sgp_goal_student (goal_id, student_id),
    INDEX idx_sgp_student (student_id),
    INDEX idx_sgp_goal (goal_id),
    CONSTRAINT fk_sgp_goal FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE,
    CONSTRAINT fk_sgp_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
);