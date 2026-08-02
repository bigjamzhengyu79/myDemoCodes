-- 题库权限：题目可见性 + 共享教师关联表
-- 说明：Hibernate ddl-auto=update 可能已自动创建下列对象，
--       请逐条执行，跳过已存在的列 / 表 / 索引
--
-- 重要：请在启动后端之前先执行本脚本。
--       ddl-auto=update 生成的 `add column visibility varchar(16) not null` 不带 DEFAULT，
--       在已有数据的 questions 表上会失败，且 Hibernate 只留一条 WARN —— 启动看似成功，
--       实际列不存在，之后所有题目查询都会报 Unknown column 'visibility'。

-- ====== 第一步：添加可见性列（带 DEFAULT，历史题目自动落为 PUBLIC，保持现有行为） ======
ALTER TABLE questions ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC' AFTER created_by;

-- ====== 第二步：兜底回填（若该列已被 ddl-auto 以可空方式加入） ======
UPDATE questions SET visibility = 'PUBLIC' WHERE visibility IS NULL OR visibility = '';

-- ====== 第三步：确保 NOT NULL ======
ALTER TABLE questions MODIFY COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC';

-- ====== 第四步：共享关联表（visibility=SHARED 时生效） ======
CREATE TABLE IF NOT EXISTS question_shares (
    question_id BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    PRIMARY KEY (question_id, user_id),
    CONSTRAINT fk_qs_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    CONSTRAINT fk_qs_user     FOREIGN KEY (user_id)     REFERENCES users(id)     ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====== 第五步：添加索引（如提示 Duplicate key name 说明已存在，跳过即可） ======
ALTER TABLE questions ADD INDEX idx_q_visibility (visibility);
ALTER TABLE questions ADD INDEX idx_q_created_by (created_by);
ALTER TABLE question_shares ADD INDEX idx_qs_user (user_id);

-- ====== 第六步：确认历史数据 ======
-- SELECT visibility, COUNT(*) FROM questions GROUP BY visibility;   -- 应全部为 PUBLIC
