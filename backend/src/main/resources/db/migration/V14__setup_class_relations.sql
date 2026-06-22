-- 1. class_groups 表增加 teacher_id 外键
ALTER TABLE class_groups ADD COLUMN teacher_id BIGINT;
ALTER TABLE class_groups ADD CONSTRAINT fk_cg_teacher
    FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE SET NULL;

-- 2. 创建班级-学生多对多关联表
CREATE TABLE IF NOT EXISTS class_group_students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_group_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cgs_group_student (class_group_id, student_id),
    INDEX idx_cgs_student (student_id),
    INDEX idx_cgs_group (class_group_id),
    CONSTRAINT fk_cgs_group FOREIGN KEY (class_group_id)
        REFERENCES class_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_cgs_student FOREIGN KEY (student_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- 3. assignments 表增加 class_group_id 外键（阶段2使用）
ALTER TABLE assignments ADD COLUMN class_group_id BIGINT;
ALTER TABLE assignments ADD CONSTRAINT fk_asn_class_group
    FOREIGN KEY (class_group_id) REFERENCES class_groups(id) ON DELETE SET NULL;

-- 4. 创建索引
CREATE INDEX idx_cg_teacher ON class_groups(teacher_id);
CREATE INDEX idx_asn_class_group ON assignments(class_group_id);