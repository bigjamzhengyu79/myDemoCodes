-- 目标-作业关联表：每个目标节点可以关联一个或多个作业
CREATE TABLE IF NOT EXISTS goal_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id BIGINT NOT NULL,
    assignment_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ga_goal_assignment (goal_id, assignment_id),
    INDEX idx_ga_goal (goal_id),
    INDEX idx_ga_assignment (assignment_id),
    CONSTRAINT fk_ga_goal FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE,
    CONSTRAINT fk_ga_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE
);