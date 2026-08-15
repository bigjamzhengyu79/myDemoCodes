-- 错题本：学生手动收藏的题目 + 订正笔记
--
-- 说明：Hibernate ddl-auto=update 可能已自动创建下列对象，
--       请逐条执行，跳过已存在的表 / 索引。
--
-- 重要：请在启动后端之前先执行本脚本。
--       参见 V21 的教训 —— ddl-auto 生成的 `add column ... not null` 不带 DEFAULT，
--       在已有数据的表上会静默失败（只留一条 WARN），启动看似成功但列并不存在。
--       本脚本所有 NOT NULL 列均显式给出 DEFAULT。
--
-- 设计说明（改动前务必阅读）：
--   本表是「学生收藏了哪些题」的唯一真相，且【完全由学生手动勾选】产生。
--   系统不会自动收录答错的题 —— 这是需求明确要求的：
--   「说是错题，实际上是做过习题的保存，只要学生勾选就能移入错题本」。
--   做对的题、蒙对的题同样可以收藏。
--
--   因此请勿"顺手"加上 score / is_wrong / error_type 等列：
--   学生的作答情况是查询时从 student_answers 实时关联的，不做快照 ——
--   教师重新批改会改分，快照会立刻与成绩单不一致。
--
--   唯一键是 (student_id, question_id) 而非 answer_id：
--   同一道题可能出现在多份作业里，但学生对它的收藏意图只有一份。

-- ====== 第一步：建表 ======
CREATE TABLE IF NOT EXISTS mistake_notes (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id           BIGINT       NOT NULL,
    question_id          BIGINT       NOT NULL,
    source_assignment_id BIGINT       NULL,
    note_content         TEXT         NULL,
    image_urls_json      MEDIUMTEXT   NULL,
    mastery              VARCHAR(20)  NOT NULL DEFAULT 'UNREVIEWED',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_mn_student_question (student_id, question_id),
    INDEX idx_mn_student (student_id),
    INDEX idx_mn_mastery (student_id, mastery),
    CONSTRAINT fk_mn_student  FOREIGN KEY (student_id)  REFERENCES users(id)     ON DELETE CASCADE,
    CONSTRAINT fk_mn_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====== 第二步：兜底（若该表已被 ddl-auto 以不同定义创建） ======
-- ddl-auto 不会加 UNIQUE 约束，也不会给 mastery 加 DEFAULT。
-- 如提示 Duplicate key name / Duplicate entry，说明已存在或有重复数据，处理后再执行。
-- ALTER TABLE mistake_notes ADD UNIQUE KEY uk_mn_student_question (student_id, question_id);
-- ALTER TABLE mistake_notes MODIFY COLUMN mastery VARCHAR(20) NOT NULL DEFAULT 'UNREVIEWED';
-- UPDATE mistake_notes SET mastery = 'UNREVIEWED' WHERE mastery IS NULL OR mastery = '';

-- ====== 第三步：关联查询的支撑索引（加在既有表上，可能已存在） ======
-- 收藏本要按 (student_id, question_id) 关联学生作答，既有表无此索引会全表扫 student_answers。
-- 如提示 Duplicate key name 说明已存在，跳过即可。
ALTER TABLE student_answers ADD INDEX idx_sa_student_question (student_id, question_id);

-- ====== 第四步：确认 ======
-- SHOW INDEX FROM mistake_notes;      -- 应含 uk_mn_student_question
-- SELECT COUNT(*) FROM mistake_notes; -- 新建时应为 0
