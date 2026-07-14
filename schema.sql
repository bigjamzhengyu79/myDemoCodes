-- ============================================================
-- 完整数据库建表SQL（合并所有Hibernate Entity + Flyway迁移）
-- 数据库类型：MySQL 8+
-- ============================================================

-- ============================================================
-- 1. users — 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(100) NOT NULL,
    email       VARCHAR(255),
    password    VARCHAR(255) NOT NULL,
    real_name   VARCHAR(64),
    role        ENUM('ADMIN','TEACHER','STUDENT') NOT NULL DEFAULT 'STUDENT',
    avatar_url  VARCHAR(255),
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME,
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 2. class_groups — 班级表
--    (V14迁移：添加teacher_id外键)
-- ============================================================
CREATE TABLE IF NOT EXISTS class_groups (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    teacher_id  BIGINT,
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME,
    UNIQUE KEY uk_cg_name (name),
    INDEX idx_cg_teacher (teacher_id),
    CONSTRAINT fk_cg_teacher FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 3. class_group_students — 班级-学生关联表（V14新建）
-- ============================================================
CREATE TABLE IF NOT EXISTS class_group_students (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_group_id  BIGINT NOT NULL,
    student_id      BIGINT NOT NULL,
    joined_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cgs_group_student (class_group_id, student_id),
    INDEX idx_cgs_student (student_id),
    INDEX idx_cgs_group (class_group_id),
    CONSTRAINT fk_cgs_group  FOREIGN KEY (class_group_id) REFERENCES class_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_cgs_student FOREIGN KEY (student_id)    REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 4. assignments — 作业表
--    (V14迁移：添加class_group_id外键)
-- ============================================================
CREATE TABLE IF NOT EXISTS assignments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    teacher_id      BIGINT NOT NULL,
    class_group_id  BIGINT,
    start_time      DATETIME,
    due_time        DATETIME,
    status          ENUM('DRAFT','PUBLISHED','CLOSED') NOT NULL DEFAULT 'DRAFT',
    created_at      DATETIME,
    INDEX idx_asn_class_group (class_group_id),
    CONSTRAINT fk_asn_teacher     FOREIGN KEY (teacher_id)     REFERENCES users(id)       ON DELETE CASCADE,
    CONSTRAINT fk_asn_class_group FOREIGN KEY (class_group_id) REFERENCES class_groups(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 5. questions — 题目表
-- ============================================================
CREATE TABLE IF NOT EXISTS questions (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    title             VARCHAR(255),
    content_latex     TEXT NOT NULL,
    question_type     ENUM('SINGLE_CHOICE','FILL_BLANK','OPEN_ENDED') NOT NULL,
    difficulty        INT NOT NULL DEFAULT 3,
    total_score       INT NOT NULL DEFAULT 5,
    answer_key        TEXT,
    source            VARCHAR(128),
    parent_id         BIGINT,
    image_urls_json   MEDIUMTEXT,
    created_by        BIGINT NOT NULL,
    created_at        DATETIME,
    updated_at        DATETIME,
    CONSTRAINT fk_q_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 6. question_options — 选择题选项表
-- ============================================================
CREATE TABLE IF NOT EXISTS question_options (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id    BIGINT NOT NULL,
    option_label   VARCHAR(1) NOT NULL,
    content_latex  TEXT NOT NULL,
    is_correct     TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_qo_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 7. solution_steps — 解题步骤表
-- ============================================================
CREATE TABLE IF NOT EXISTS solution_steps (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id     BIGINT NOT NULL,
    step_order      INT NOT NULL,
    content_latex   TEXT NOT NULL,
    step_score      INT NOT NULL DEFAULT 0,
    common_errors   TEXT,
    image_urls_json MEDIUMTEXT,
    INDEX idx_ss_question (question_id),
    CONSTRAINT fk_ss_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 8. knowledge_tags — 知识标签表
-- ============================================================
CREATE TABLE IF NOT EXISTS knowledge_tags (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id  BIGINT,
    name       VARCHAR(64) NOT NULL,
    chapter    VARCHAR(64),
    sort_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 9. student_answers — 学生作答表
-- ============================================================
CREATE TABLE IF NOT EXISTS student_answers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id   BIGINT NOT NULL,
    question_id     BIGINT NOT NULL,
    student_id      BIGINT NOT NULL,
    answer_content  TEXT,
    image_urls_json MEDIUMTEXT,
    score           INT,
    auto_score      INT,
    feedback        TEXT,
    error_type      ENUM('CONCEPT','CALC','READING','NONE'),
    status          ENUM('DRAFT','SUBMITTED','AUTO_GRADED','REVIEWED') NOT NULL DEFAULT 'SUBMITTED',
    submitted_at    DATETIME,
    reviewed_at     DATETIME,
    reviewer_id     BIGINT,
    INDEX idx_sa_assignment (assignment_id),
    INDEX idx_sa_question (question_id),
    INDEX idx_sa_student (student_id),
    CONSTRAINT fk_sa_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_sa_question   FOREIGN KEY (question_id)   REFERENCES questions(id)   ON DELETE CASCADE,
    CONSTRAINT fk_sa_student    FOREIGN KEY (student_id)    REFERENCES users(id)       ON DELETE CASCADE,
    CONSTRAINT fk_sa_reviewer   FOREIGN KEY (reviewer_id)   REFERENCES users(id)       ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 10. assignment_questions — 作业-题目关联表（ManyToMany）
-- ============================================================
CREATE TABLE IF NOT EXISTS assignment_questions (
    assignment_id BIGINT NOT NULL,
    question_id   BIGINT NOT NULL,
    PRIMARY KEY (assignment_id, question_id),
    CONSTRAINT fk_aq_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_aq_question   FOREIGN KEY (question_id)   REFERENCES questions(id)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 11. question_knowledge_tags — 题目-标签关联表（ManyToMany）
-- ============================================================
CREATE TABLE IF NOT EXISTS question_knowledge_tags (
    question_id BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    PRIMARY KEY (question_id, tag_id),
    CONSTRAINT fk_qkt_question FOREIGN KEY (question_id) REFERENCES questions(id)      ON DELETE CASCADE,
    CONSTRAINT fk_qkt_tag      FOREIGN KEY (tag_id)      REFERENCES knowledge_tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 12. goals — 目标表
--     (V16迁移：添加class_group_id外键)
-- ============================================================
CREATE TABLE IF NOT EXISTS goals (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'TODO',
    planned_start   DATE,
    planned_end     DATE,
    actual_start    DATE,
    actual_end      DATE,
    progress        INT NOT NULL DEFAULT 0,
    parent_id       BIGINT,
    class_group_id  BIGINT,
    copyable        BOOLEAN NOT NULL DEFAULT FALSE,
    owners          VARCHAR(500),
    manager_id      BIGINT,
    assignee_id     BIGINT,
    depth           INT NOT NULL DEFAULT 1,
    created_at      DATETIME,
    updated_at      DATETIME,
    INDEX idx_goal_parent (parent_id),
    INDEX idx_goal_class_group (class_group_id),
    CONSTRAINT fk_goal_parent      FOREIGN KEY (parent_id)      REFERENCES goals(id)        ON DELETE CASCADE,
    CONSTRAINT fk_goal_class_group FOREIGN KEY (class_group_id) REFERENCES class_groups(id)  ON DELETE SET NULL,
    CONSTRAINT fk_goal_manager     FOREIGN KEY (manager_id)     REFERENCES users(id)         ON DELETE SET NULL,
    CONSTRAINT fk_goal_assignee    FOREIGN KEY (assignee_id)    REFERENCES users(id)         ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 13. goal_assignees — 目标-学生分配表（V13新建）
-- ============================================================
CREATE TABLE IF NOT EXISTS goal_assignees (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id     BIGINT NOT NULL,
    student_id  BIGINT NOT NULL,
    assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ga_goal_student (goal_id, student_id),
    INDEX idx_ga_student (student_id),
    INDEX idx_ga_goal (goal_id),
    CONSTRAINT fk_ga_goal    FOREIGN KEY (goal_id)    REFERENCES goals(id) ON DELETE CASCADE,
    CONSTRAINT fk_ga_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 14. student_goal_progress — 学生个人目标进度表
--     (V17新建, V18扩展：添加actual_start, actual_end)
-- ============================================================
CREATE TABLE IF NOT EXISTS student_goal_progress (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id      BIGINT NOT NULL,
    student_id   BIGINT NOT NULL,
    progress     INT NOT NULL DEFAULT 0,
    status       VARCHAR(20) NOT NULL DEFAULT 'TODO',
    actual_start DATE,
    actual_end   DATE,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sgp_goal_student (goal_id, student_id),
    INDEX idx_sgp_student (student_id),
    INDEX idx_sgp_goal (goal_id),
    CONSTRAINT fk_sgp_goal    FOREIGN KEY (goal_id)    REFERENCES goals(id) ON DELETE CASCADE,
    CONSTRAINT fk_sgp_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 15. goal_comments — 目标评论表
--     (V18新建, V20改造：添加author_id/author_role/visibility/target_student_id)
-- ============================================================
CREATE TABLE IF NOT EXISTS goal_comments (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id           BIGINT NOT NULL,
    student_id        BIGINT,
    author_id         BIGINT NOT NULL,
    author_role       VARCHAR(10) NOT NULL DEFAULT 'STUDENT',
    visibility        VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    target_student_id BIGINT,
    content           TEXT NOT NULL COMMENT '评论内容，支持LaTeX富文本',
    image_urls        TEXT COMMENT '图片URL列表，JSON数组格式',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_gc_goal (goal_id),
    INDEX idx_gc_student (student_id),
    INDEX idx_gc_author (author_id),
    INDEX idx_gc_visibility (visibility),
    INDEX idx_gc_target (target_student_id),
    CONSTRAINT fk_gc_goal   FOREIGN KEY (goal_id)   REFERENCES goals(id) ON DELETE CASCADE,
    CONSTRAINT fk_gc_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 16. goal_assignments — 目标-作业关联表（V19新建）
-- ============================================================
CREATE TABLE IF NOT EXISTS goal_assignments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id        BIGINT NOT NULL,
    assignment_id  BIGINT NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ga_goal_assignment (goal_id, assignment_id),
    INDEX idx_ga_goal (goal_id),
    INDEX idx_ga_assignment (assignment_id),
    CONSTRAINT fk_ga_goal       FOREIGN KEY (goal_id)       REFERENCES goals(id)       ON DELETE CASCADE,
    CONSTRAINT fk_ga_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;