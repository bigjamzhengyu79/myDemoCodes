-- ============================================================
--  高中数学习题系统 - 数据库设计
-- ============================================================

CREATE DATABASE IF NOT EXISTS math_homework DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE math_homework;

-- ------------------------------------------------------------
-- 用户表
-- ------------------------------------------------------------
CREATE TABLE users (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(64) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    real_name   VARCHAR(64),
    role        ENUM('TEACHER','STUDENT') NOT NULL DEFAULT 'STUDENT',
    class_name  VARCHAR(64),
    avatar_url  VARCHAR(255),
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 知识点标签表（支持树形结构）
-- ------------------------------------------------------------
CREATE TABLE knowledge_tags (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT DEFAULT NULL,
    name        VARCHAR(64) NOT NULL,
    chapter     VARCHAR(64),
    sort_order  INT DEFAULT 0,
    FOREIGN KEY (parent_id) REFERENCES knowledge_tags(id)
);

-- ------------------------------------------------------------
-- 题目主表
-- ------------------------------------------------------------
CREATE TABLE questions (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    title           VARCHAR(255),
    content_latex   TEXT NOT NULL COMMENT '题干LaTeX',
    question_type   ENUM('SINGLE_CHOICE','FILL_BLANK','OPEN_ENDED') NOT NULL,
    difficulty      TINYINT NOT NULL DEFAULT 3 COMMENT '1-5',
    total_score     INT NOT NULL DEFAULT 5,
    answer_key      TEXT COMMENT '标准答案',
    source          VARCHAR(128) COMMENT '来源（如2024全国甲卷）',
    parent_id       BIGINT DEFAULT NULL COMMENT '变式题父题ID',
    created_by      BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (parent_id)  REFERENCES questions(id)
);

-- ------------------------------------------------------------
-- 题目与知识点 多对多
-- ------------------------------------------------------------
CREATE TABLE question_knowledge_tags (
    question_id BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    PRIMARY KEY (question_id, tag_id),
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id)      REFERENCES knowledge_tags(id)
);

-- ------------------------------------------------------------
-- 解析步骤表
-- ------------------------------------------------------------
CREATE TABLE solution_steps (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id     BIGINT NOT NULL,
    step_order      INT NOT NULL,
    content_latex   TEXT NOT NULL,
    step_score      INT NOT NULL DEFAULT 0,
    common_errors   TEXT,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 选项表（单选题）
-- ------------------------------------------------------------
CREATE TABLE question_options (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id     BIGINT NOT NULL,
    option_label    CHAR(1) NOT NULL COMMENT 'A/B/C/D',
    content_latex   TEXT NOT NULL,
    is_correct      TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 作业表
-- ------------------------------------------------------------
CREATE TABLE assignments (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    teacher_id      BIGINT NOT NULL,
    class_name      VARCHAR(64),
    start_time      DATETIME,
    due_time        DATETIME,
    status          ENUM('DRAFT','PUBLISHED','CLOSED') NOT NULL DEFAULT 'DRAFT',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (teacher_id) REFERENCES users(id)
);

-- ------------------------------------------------------------
-- 作业题目关联
-- ------------------------------------------------------------
CREATE TABLE assignment_questions (
    assignment_id   BIGINT NOT NULL,
    question_id     BIGINT NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    PRIMARY KEY (assignment_id, question_id),
    FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id)   REFERENCES questions(id)
);

-- ------------------------------------------------------------
-- 学生答题记录表
-- ------------------------------------------------------------
CREATE TABLE student_answers (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id   BIGINT NOT NULL,
    question_id     BIGINT NOT NULL,
    student_id      BIGINT NOT NULL,
    answer_content  TEXT COMMENT '学生答案（文字/LaTeX）',
    image_url       VARCHAR(255) COMMENT '拍照上传图片URL',
    score           INT DEFAULT NULL COMMENT '得分（null表示未批改）',
    auto_score      INT DEFAULT NULL COMMENT 'AI建议分',
    feedback        TEXT COMMENT '批改反馈',
    error_type      ENUM('CONCEPT','CALC','READING','NONE') DEFAULT NULL,
    status          ENUM('DRAFT','SUBMITTED','AUTO_GRADED','REVIEWED') NOT NULL DEFAULT 'SUBMITTED',
    submitted_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at     DATETIME DEFAULT NULL,
    reviewer_id     BIGINT DEFAULT NULL,
    FOREIGN KEY (assignment_id) REFERENCES assignments(id),
    FOREIGN KEY (question_id)   REFERENCES questions(id),
    FOREIGN KEY (student_id)    REFERENCES users(id),
    FOREIGN KEY (reviewer_id)   REFERENCES users(id),
    UNIQUE KEY uq_answer (assignment_id, question_id, student_id)
);

-- ------------------------------------------------------------
-- 错题本
-- ------------------------------------------------------------
CREATE TABLE mistake_book (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id      BIGINT NOT NULL,
    question_id     BIGINT NOT NULL,
    answer_id       BIGINT NOT NULL,
    error_type      VARCHAR(32),
    mastered        TINYINT(1) NOT NULL DEFAULT 0,
    added_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mastered_at     DATETIME DEFAULT NULL,
    FOREIGN KEY (student_id)  REFERENCES users(id),
    FOREIGN KEY (question_id) REFERENCES questions(id),
    FOREIGN KEY (answer_id)   REFERENCES student_answers(id),
    UNIQUE KEY uq_mistake (student_id, question_id)
);

-- ============================================================
-- 初始数据
-- ============================================================

-- 默认用户（密码均为 123456，BCrypt加密）
INSERT INTO users (username, password, real_name, role, class_name) VALUES
('teacher01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '张老师', 'TEACHER', NULL),
('student01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '李明', 'STUDENT', '高三(1)班'),
('student02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '王芳', 'STUDENT', '高三(1)班');

-- 知识点
INSERT INTO knowledge_tags (id, parent_id, name, chapter, sort_order) VALUES
(1, NULL, '导数', '微积分初步', 1),
(2, 1,    '导数计算', '微积分初步', 1),
(3, 1,    '导数应用', '微积分初步', 2),
(4, NULL, '数列', '数列', 2),
(5, 4,    '等差数列', '数列', 1),
(6, 4,    '等比数列', '数列', 2),
(7, NULL, '不等式', '不等式', 3),
(8, 7,    '不等式证明', '不等式', 1);

-- 题目：单选
INSERT INTO questions (id, title, content_latex, question_type, difficulty, total_score, answer_key, source, created_by) VALUES
(1, '导数极小值', '函数 $f(x) = x^3 - 3x$ 的极小值为', 'SINGLE_CHOICE', 2, 5, 'A', '2024全国甲卷', 1);

INSERT INTO question_options (question_id, option_label, content_latex, is_correct) VALUES
(1, 'A', '$-2$', 1),
(1, 'B', '$2$', 0),
(1, 'C', '$0$', 0),
(1, 'D', '$-6$', 0);

INSERT INTO question_knowledge_tags VALUES (1, 2), (1, 3);

-- 题目：填空
INSERT INTO questions (id, title, content_latex, question_type, difficulty, total_score, answer_key, source, created_by) VALUES
(2, '等差数列求项', '等差数列 $\\{a_n\\}$ 中，$a_3 + a_7 = 20$，则 $a_5 =$ ___', 'FILL_BLANK', 2, 5, '10', '校本练习', 1);

INSERT INTO question_knowledge_tags VALUES (2, 5);

-- 题目：解答
INSERT INTO questions (id, title, content_latex, question_type, difficulty, total_score, answer_key, source, created_by) VALUES
(3, '导数与不等式证明', '已知函数 $f(x) = \\ln x + \\dfrac{1-x}{1+x}$\n\n（1）讨论 $f(x)$ 的单调性；\n\n（2）证明：当 $x > 0$ 时，$\\ln x \\geq \\dfrac{2(x-1)}{x+1}$', 'OPEN_ENDED', 4, 12, NULL, '2023全国乙卷', 1);

INSERT INTO solution_steps (question_id, step_order, content_latex, step_score, common_errors) VALUES
(3, 1, '求导：$f\'(x) = \\dfrac{1}{x} - \\dfrac{2}{(1+x)^2} = \\dfrac{(x-1)^2}{x(1+x)^2}$', 2, '忘记限制定义域 $x>0$'),
(3, 2, '因 $x > 0$ 时 $f\'(x) \\geq 0$，故 $f(x)$ 在 $(0, +\\infty)$ 上单调递增', 2, '误判等号条件'),
(3, 3, '令 $x > 0$，则 $f(x) \\geq f(1) = 0$', 2, '忘记验证 $f(1)=0$'),
(3, 4, '即 $\\ln x + \\dfrac{1-x}{1+x} \\geq 0$，整理得 $\\ln x \\geq \\dfrac{2(x-1)}{x+1}$（证毕）', 2, '整理步骤不规范');

INSERT INTO question_knowledge_tags VALUES (3, 2), (3, 3), (3, 8);

-- 作业
INSERT INTO assignments (id, title, description, teacher_id, class_name, due_time, status) VALUES
(1, '导数专项练习 #1', '重点练习导数计算与应用，包含不等式证明', 1, '高三(1)班', DATE_ADD(NOW(), INTERVAL 3 DAY), 'PUBLISHED');

INSERT INTO assignment_questions VALUES (1, 1, 1), (1, 2, 2), (1, 3, 3);
