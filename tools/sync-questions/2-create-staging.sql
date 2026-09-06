-- ============================================================
-- 步骤2：在【线上库】建暂存表 + ID 映射表
--
-- stg_* 结构与正式表一致，但：
--   * 无任何外键（本地 ID 在线上无意义，带外键必然导入失败）
--   * 无 AUTO_INCREMENT（要原样保留本地 ID 作为映射来源）
--
-- TiDB 说明：
--   * 排序规则必须与线上正式表一致（utf8mb4_0900_ai_ci）。
--     线上表是 Hibernate 建的，用的就是这个；若这里写 unicode_ci，
--     stg_* 与正式表 JOIN 时会报 Illegal mix of collations。
--   * ENGINE=InnoDB 会被解析但忽略（TiDB 只有自己的存储引擎），
--     保留写法只为与本地 schema.sql 保持一致，无副作用。
--   * TiDB 的 AUTO_INCREMENT 是按 tidb_server 分配区间的，
--     生成的 ID 不连续、也不保证单调递增 —— 这正是本方案
--     不靠「插入后取 LAST_INSERT_ID」而靠 (作者,题干) 回查映射的原因。
--     参见 3-merge.sql 第2步的卡点0（题干重复检查）。
-- ============================================================

CREATE TABLE IF NOT EXISTS stg_questions (
    id                BIGINT PRIMARY KEY,
    title             VARCHAR(255),
    content_latex     TEXT NOT NULL,
    question_type     VARCHAR(32) NOT NULL,
    difficulty        INT NOT NULL DEFAULT 3,
    total_score       INT NOT NULL DEFAULT 5,
    answer_key        TEXT,
    source            VARCHAR(128),
    parent_id         BIGINT,
    image_urls_json   MEDIUMTEXT,
    created_by        BIGINT NOT NULL,
    visibility        VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
    created_at        DATETIME,
    updated_at        DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS stg_question_options (
    id             BIGINT PRIMARY KEY,
    question_id    BIGINT NOT NULL,
    option_label   VARCHAR(1) NOT NULL,
    content_latex  TEXT NOT NULL,
    is_correct     TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_stg_qo_q (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS stg_solution_steps (
    id              BIGINT PRIMARY KEY,
    question_id     BIGINT NOT NULL,
    step_order      INT NOT NULL,
    content_latex   TEXT NOT NULL,
    step_score      INT NOT NULL DEFAULT 0,
    common_errors   TEXT,
    image_urls_json MEDIUMTEXT,
    INDEX idx_stg_ss_q (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS stg_knowledge_tags (
    id         BIGINT PRIMARY KEY,
    parent_id  BIGINT,
    name       VARCHAR(64) NOT NULL,
    chapter    VARCHAR(64),
    sort_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS stg_question_knowledge_tags (
    question_id BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    PRIMARY KEY (question_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS stg_question_shares (
    question_id BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    PRIMARY KEY (question_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---- ID 映射表：本地 ID -> 线上新 ID ----
-- 这是整个方案的核心。所有子表的 question_id 都靠它翻译。
CREATE TABLE IF NOT EXISTS map_question (
    local_id  BIGINT PRIMARY KEY,
    online_id BIGINT,
    INDEX idx_map_q_online (online_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS map_tag (
    local_id  BIGINT PRIMARY KEY,
    online_id BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 本地 user id -> 线上 user id，按 username 对齐（见 3-merge.sql 第0步）
CREATE TABLE IF NOT EXISTS map_user (
    local_id  BIGINT PRIMARY KEY,
    online_id BIGINT,
    username  VARCHAR(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
