-- ============================================
-- Math Goals Management System
-- MySQL 初始化脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS aiproject CHARACTER SET utf8 COLLATE utf8_general_ci;
USE aiproject;
SET NAMES utf8;
SET character_set_client = 'utf8';
SET character_set_connection = 'utf8';
SET character_set_results = 'utf8';

-- 重建 app_users 表（旧用户管理模块）
DROP TABLE IF EXISTS app_users;
CREATE TABLE app_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

-- 重建 math_goals 表
DROP TABLE IF EXISTS math_goals;
CREATE TABLE math_goals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    target_date DATE,
    progress INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT '进行中',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

-- 创建索引以提高查询性能
CREATE INDEX idx_math_goals_status ON math_goals(status);
CREATE INDEX idx_math_goals_category ON math_goals(category);
CREATE INDEX idx_math_goals_created_at ON math_goals(created_at);

-- 插入示例数据
INSERT IGNORE INTO math_goals (id, title, description, category, target_date, progress, status, created_at, updated_at)
VALUES 
    (1, '掌握微积分基础', '学习导数、积分和极限的基本概念', '微积分', '2026-06-30', 45, '进行中', NOW(), NOW()),
    (2, '平面几何证明技巧', '掌握常见的平面几何证明方法和技巧', '几何', '2026-05-31', 60, '进行中', NOW(), NOW()),
    (3, '线性代数入门', '理解向量、矩阵和线性变换的基本概念', '代数', '2026-07-31', 30, '进行中', NOW(), NOW()),
    (4, '概率论基础', '学习基本的概率分布和统计推断', '概率', '2026-08-31', 25, '进行中', NOW(), NOW()),
    (5, '完成高等数学第一章', '完成教科书的第一章所有练习题', '其他', '2026-04-30', 100, '已完成', NOW(), NOW());

-- 插入旧用户管理模块基础用户
INSERT IGNORE INTO app_users (username, email, password, created_at, updated_at) VALUES
('zhangsan', 'zhangsan@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', NOW(), NOW());

-- 验证表创建成功
SELECT 'Math Goals 表创建成功！' as 信息;
SELECT COUNT(*) as '已插入示例数据行数' FROM math_goals;

-- 查看数据表结构
DESC math_goals;

-- 查看已插入的数据
SELECT * FROM math_goals;

-- ============================================================
-- 高中数学习题系统表（由 schema_homework.sql 合并）
-- ============================================================

-- 使用同一个数据库
USE aiproject;

DROP TABLE IF EXISTS mistake_book;
DROP TABLE IF EXISTS student_answers;
DROP TABLE IF EXISTS assignment_questions;
DROP TABLE IF EXISTS assignments;
DROP TABLE IF EXISTS question_options;
DROP TABLE IF EXISTS solution_steps;
DROP TABLE IF EXISTS question_knowledge_tags;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS knowledge_tags;
DROP TABLE IF EXISTS users;

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(64) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    real_name   VARCHAR(64),
    role        ENUM('TEACHER','STUDENT') NOT NULL DEFAULT 'STUDENT',
    class_name  VARCHAR(64),
    avatar_url  VARCHAR(255),
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_tags (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT DEFAULT NULL,
    name        VARCHAR(64) NOT NULL,
    chapter     VARCHAR(64),
    sort_order  INT DEFAULT 0,
    FOREIGN KEY (parent_id) REFERENCES knowledge_tags(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS questions (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    title           VARCHAR(255),
    content_latex   TEXT NOT NULL,
    question_type   ENUM('SINGLE_CHOICE','FILL_BLANK','OPEN_ENDED') NOT NULL,
    difficulty      TINYINT NOT NULL DEFAULT 3,
    total_score     INT NOT NULL DEFAULT 5,
    answer_key      TEXT,
    source          VARCHAR(128),
    parent_id       BIGINT DEFAULT NULL,
    created_by      BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (parent_id) REFERENCES questions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 增量迁移：为 questions 表增加图片字段
-- （首次建库已包含在 CREATE TABLE 中无需重复执行，
--  仅在已有数据库上增量执行此段）
-- ============================================================
ALTER TABLE questions
  ADD COLUMN image_urls_json MEDIUMTEXT DEFAULT NULL
  COMMENT '题目配图（JSON 数组，Base64 DataURL）';

CREATE TABLE IF NOT EXISTS question_knowledge_tags (
    question_id BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    PRIMARY KEY (question_id, tag_id),
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id)      REFERENCES knowledge_tags(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS solution_steps (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id     BIGINT NOT NULL,
    step_order      INT NOT NULL,
    content_latex   TEXT NOT NULL,
    step_score      INT NOT NULL DEFAULT 0,
    common_errors   TEXT,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS question_options (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id     BIGINT NOT NULL,
    option_label    CHAR(1) NOT NULL,
    content_latex   TEXT NOT NULL,
    is_correct      TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assignments (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assignment_questions (
    assignment_id   BIGINT NOT NULL,
    question_id     BIGINT NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    PRIMARY KEY (assignment_id, question_id),
    FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id)   REFERENCES questions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_answers (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id   BIGINT NOT NULL,
    question_id     BIGINT NOT NULL,
    student_id      BIGINT NOT NULL,
    answer_content  TEXT,
    image_url       VARCHAR(255),
    score           INT DEFAULT NULL,
    auto_score      INT DEFAULT NULL,
    feedback        TEXT,
    error_type      ENUM('CONCEPT','CALC','READING','NONE') DEFAULT NULL,
    status          ENUM('SUBMITTED','AUTO_GRADED','REVIEWED') NOT NULL DEFAULT 'SUBMITTED',
    submitted_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at     DATETIME DEFAULT NULL,
    reviewer_id     BIGINT DEFAULT NULL,
    FOREIGN KEY (assignment_id) REFERENCES assignments(id),
    FOREIGN KEY (question_id)   REFERENCES questions(id),
    FOREIGN KEY (student_id)    REFERENCES users(id),
    FOREIGN KEY (reviewer_id)   REFERENCES users(id),
    UNIQUE KEY uq_answer (assignment_id, question_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mistake_book (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 初始数据
INSERT IGNORE INTO users (username, password, real_name, role, class_name) VALUES
('teacher01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '张老师', 'TEACHER', NULL),
('student01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '李明', 'STUDENT', '高三(1)班');

INSERT IGNORE INTO knowledge_tags (id, parent_id, name, chapter, sort_order) VALUES
(1, NULL, '导数', '微积分初步', 1),
(2, 1,    '导数计算', '微积分初步', 1),
(3, 1,    '导数应用', '微积分初步', 2),
(4, NULL, '数列', '数列', 2),
(5, 4,    '等差数列', '数列', 1),
(6, 4,    '等比数列', '数列', 2),
(7, NULL, '不等式', '不等式', 3),
(8, 7,    '不等式证明', '不等式', 1);

INSERT IGNORE INTO questions (id, title, content_latex, question_type, difficulty, total_score, answer_key, source, created_by) VALUES
(1, '导数极小值', '函数 $f(x) = x^3 - 3x$ 的极小值为', 'SINGLE_CHOICE', 2, 5, 'A', '2024全国甲卷', 1),
(2, '等差数列求项', '等差数列 $\\{a_n\\}$ 中，$a_3 + a_7 = 20$，则 $a_5 =$ ___', 'FILL_BLANK', 2, 5, '10', '校本练习', 1),
(3, '导数与不等式证明', '已知函数 $f(x) = \\ln x + \\dfrac{1-x}{1+x}$\n\n（1）讨论 $f(x)$ 的单调性；\n\n（2）证明：当 $x > 0$ 时，$\\ln x \\geq \\dfrac{2(x-1)}{x+1}$', 'OPEN_ENDED', 4, 12, NULL, '2023全国乙卷', 1);

INSERT IGNORE INTO question_options (question_id, option_label, content_latex, is_correct) VALUES
(1, 'A', '$-2$', 1),
(1, 'B', '$2$', 0),
(1, 'C', '$0$', 0),
(1, 'D', '$-6$', 0);

INSERT IGNORE INTO question_knowledge_tags VALUES (1, 2), (1, 3), (2, 5), (3, 2), (3, 3), (3, 8);

INSERT IGNORE INTO solution_steps (question_id, step_order, content_latex, step_score, common_errors) VALUES
(3, 1, '求导：$f\'x = \\dfrac{1}{x} - \\dfrac{2}{(1+x)^2} = \\dfrac{(x-1)^2}{x(1+x)^2}$', 2, '忘记限制定义域 $x>0$'),
(3, 2, '因 $x > 0$ 时 $f\'x \\geq 0$，故 $f(x)$ 在 $(0, +\\infty)$ 上单调递增', 2, '误判等号条件'),
(3, 3, '令 $x > 0$，则 $f(x) \\geq f(1) = 0$', 2, '忘记验证 $f(1)=0$'),
(3, 4, '即 $\\ln x + \\dfrac{1-x}{1+x} \\geq 0$，整理得 $\\ln x \\geq \\dfrac{2(x-1)}{x+1}$（证毕）', 2, '整理步骤不规范');

INSERT IGNORE INTO assignments (id, title, description, teacher_id, class_name, due_time, status) VALUES
(1, '导数专项练习 #1', '重点练习导数计算与应用，包含不等式证明', 1, '高三(1)班', DATE_ADD(NOW(), INTERVAL 3 DAY), 'PUBLISHED');

INSERT IGNORE INTO assignment_questions VALUES (1, 1, 1), (1, 2, 2), (1, 3, 3);

SELECT '作业模块表创建完成';
SELECT COUNT(*) AS count_users FROM users;
SELECT COUNT(*) AS count_assignments FROM assignments;
