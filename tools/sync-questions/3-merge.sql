-- ============================================================
-- 步骤3：在【线上库】把 stg_* 合并进正式表
--
-- 全程在一个事务里跑。任何一步报错就 ROLLBACK，线上数据不受影响。
-- 顺序不可调换：每一步都依赖上一步产生的映射。
-- ============================================================

-- 事务由外部注入（见 4-load-online.sh）：
--   演练模式在文件末尾追加 ROLLBACK，提交模式追加 COMMIT。
-- 这里不写 START TRANSACTION，避免 mysql 客户端读到 EOF 时自动提交。
START TRANSACTION;

-- ============================================================
-- 第0步：对齐用户
-- 按 username 把本地 user id 翻译成线上 user id。
-- ============================================================
UPDATE map_user m
JOIN users u ON u.username = m.username
SET m.online_id = u.id;

-- 【卡点1】没对上的**出题人** —— 必须为 0 行才能继续。
--
-- 只检查 stg_questions.created_by 实际引用到的用户：
-- 本地库里的学生账号（student01..06）和 teacher02 与题库无关，
-- 线上没有它们完全正常，不该因此卡住流程。
-- 真正致命的是「某道题的作者在线上不存在」—— 那样题目无法归属。
--
-- 处理：先在线上建出该教师账号，再重跑。
SELECT DISTINCT m.local_id, m.username AS '未在线上找到的出题人'
FROM map_user m
JOIN stg_questions s ON s.created_by = m.local_id
WHERE m.online_id IS NULL;

-- 参考信息：本地有、线上无，但与题库无关的账号（不影响本次同步）
SELECT m.local_id, m.username AS '仅本地存在的账号（可忽略）'
FROM map_user m
WHERE m.online_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM stg_questions s WHERE s.created_by = m.local_id);


-- ============================================================
-- 第1步：合并知识点标签（questions 的弱依赖，先做）
-- 按 name 去重：线上已有同名标签就复用，没有才新建。
-- ============================================================

-- 1a. 先为每个本地标签建一条空映射
INSERT INTO map_tag (local_id, online_id)
SELECT s.id, NULL FROM stg_knowledge_tags s
ON DUPLICATE KEY UPDATE local_id = map_tag.local_id;

-- 1b. 线上已有同名标签的，直接复用其 ID
UPDATE map_tag m
JOIN stg_knowledge_tags s ON s.id = m.local_id
JOIN knowledge_tags k ON k.name = s.name
SET m.online_id = k.id
WHERE m.online_id IS NULL;

-- 1c. 线上没有的标签，新建
INSERT INTO knowledge_tags (parent_id, name, chapter, sort_order)
SELECT NULL, s.name, s.chapter, s.sort_order
FROM stg_knowledge_tags s
JOIN map_tag m ON m.local_id = s.id
WHERE m.online_id IS NULL;

-- 1d. 回填新建标签的映射
UPDATE map_tag m
JOIN stg_knowledge_tags s ON s.id = m.local_id
JOIN knowledge_tags k ON k.name = s.name
SET m.online_id = k.id
WHERE m.online_id IS NULL;

-- 1e. 标签的父子关系用映射后的 ID 重建
UPDATE knowledge_tags k
JOIN map_tag mt   ON mt.online_id = k.id
JOIN stg_knowledge_tags s ON s.id = mt.local_id
JOIN map_tag mp   ON mp.local_id = s.parent_id
SET k.parent_id = mp.online_id
WHERE s.parent_id IS NOT NULL AND mp.online_id IS NOT NULL;


-- ============================================================
-- 第2步：插入题目主体，拿到线上新 ID
--
-- 注意 parent_id 先写 NULL —— 本地父题的线上 ID 此刻还不存在，
-- 等全部插完再回填（第3步）。这是自引用外键的标准处理方式。
--
-- 去重策略：跳过线上已存在的题。判定口径 = 同作者 + 同题干。
-- 若你的口径不同（比如允许同题干重复），删掉 WHERE NOT EXISTS 即可。
-- ============================================================
-- ------------------------------------------------------------
-- 【卡点0】题干重复检查 —— 必须为 0 行。
--
-- 本步骤靠 (作者, 题干) 这组值来「找回」刚插入的题的新 ID。
-- 如果同一作者名下存在两道题干完全相同的题，这个 JOIN 会一对多展开，
-- 映射就会错乱（选项/步骤会挂到错误的题上）。
-- 有输出时不要继续，先在本地把重复题合并或改掉题干。
-- ------------------------------------------------------------
SELECT s.created_by AS 本地作者ID, COUNT(*) AS 重复条数,
       LEFT(s.content_latex, 40) AS 题干片段
FROM stg_questions s
GROUP BY s.created_by, s.content_latex
HAVING COUNT(*) > 1;

INSERT INTO questions
    (title, content_latex, question_type, difficulty, total_score,
     answer_key, source, parent_id, image_urls_json, created_by,
     visibility, created_at, updated_at)
SELECT
    s.title, s.content_latex, s.question_type, s.difficulty, s.total_score,
    s.answer_key, s.source, NULL, s.image_urls_json, mu.online_id,
    s.visibility, s.created_at, s.updated_at
FROM stg_questions s
JOIN map_user mu ON mu.local_id = s.created_by AND mu.online_id IS NOT NULL
WHERE NOT EXISTS (
    SELECT 1 FROM questions q
    WHERE q.created_by = mu.online_id
      AND q.content_latex = s.content_latex
)
ORDER BY s.id;

-- 回填映射：新插入的题按 (作者, 题干) 找回线上 ID
INSERT INTO map_question (local_id, online_id)
SELECT s.id, q.id
FROM stg_questions s
JOIN map_user mu ON mu.local_id = s.created_by AND mu.online_id IS NOT NULL
JOIN questions q ON q.created_by = mu.online_id
                AND q.content_latex = s.content_latex
ON DUPLICATE KEY UPDATE online_id = VALUES(online_id);

-- 【卡点2】没能映射上的题 —— 必须为 0 行。
-- 有输出通常意味着该题作者在线上不存在（见卡点1）。
SELECT s.id AS '未映射的本地题目ID', LEFT(s.content_latex, 40) AS '题干片段'
FROM stg_questions s
LEFT JOIN map_question m ON m.local_id = s.id
WHERE m.online_id IS NULL;


-- ============================================================
-- 第3步：回填题目的父子关系（组合题 / 子题）
-- ============================================================
UPDATE questions q
JOIN map_question mq ON mq.online_id = q.id
JOIN stg_questions s ON s.id = mq.local_id
JOIN map_question mp ON mp.local_id = s.parent_id
SET q.parent_id = mp.online_id
WHERE s.parent_id IS NOT NULL AND mp.online_id IS NOT NULL;


-- ============================================================
-- 第4步：选项 / 解题步骤
--
-- 先删后插：只针对本次映射到的题。
-- 这样重复执行整个脚本是幂等的，不会产生重复选项。
-- 注意作用域被 map_question 限死，绝不会碰到线上其它题目的选项。
-- ============================================================
DELETE qo FROM question_options qo
JOIN map_question m ON m.online_id = qo.question_id;

INSERT INTO question_options (question_id, option_label, content_latex, is_correct)
SELECT m.online_id, s.option_label, s.content_latex, s.is_correct
FROM stg_question_options s
JOIN map_question m ON m.local_id = s.question_id
WHERE m.online_id IS NOT NULL;

DELETE ss FROM solution_steps ss
JOIN map_question m ON m.online_id = ss.question_id;

INSERT INTO solution_steps (question_id, step_order, content_latex, step_score, common_errors, image_urls_json)
SELECT m.online_id, s.step_order, s.content_latex, s.step_score, s.common_errors, s.image_urls_json
FROM stg_solution_steps s
JOIN map_question m ON m.local_id = s.question_id
WHERE m.online_id IS NOT NULL;


-- ============================================================
-- 第5步：题目-标签关联、共享关系
-- 用 INSERT IGNORE：主键是复合键，重复执行自动跳过。
-- ============================================================
INSERT IGNORE INTO question_knowledge_tags (question_id, tag_id)
SELECT mq.online_id, mt.online_id
FROM stg_question_knowledge_tags s
JOIN map_question mq ON mq.local_id = s.question_id
JOIN map_tag      mt ON mt.local_id = s.tag_id
WHERE mq.online_id IS NOT NULL AND mt.online_id IS NOT NULL;

-- 共享关系：被共享的教师在线上不存在就自动跳过（JOIN 过滤掉）
INSERT IGNORE INTO question_shares (question_id, user_id)
SELECT mq.online_id, mu.online_id
FROM stg_question_shares s
JOIN map_question mq ON mq.local_id = s.question_id
JOIN map_user     mu ON mu.local_id = s.user_id
WHERE mq.online_id IS NOT NULL AND mu.online_id IS NOT NULL;


-- ============================================================
-- 第6步：核对。确认无误后再手动 COMMIT。
-- ============================================================
SELECT
  (SELECT COUNT(*) FROM stg_questions)                                    AS '本地题目数',
  (SELECT COUNT(*) FROM map_question WHERE online_id IS NOT NULL)         AS '已映射数',
  (SELECT COUNT(*) FROM questions)                                        AS '线上题目总数',
  (SELECT COUNT(*) FROM question_options qo JOIN map_question m ON m.online_id = qo.question_id) AS '本次选项数',
  (SELECT COUNT(*) FROM solution_steps ss JOIN map_question m ON m.online_id = ss.question_id)   AS '本次步骤数';

-- 确认上面数字合理后执行：
--   COMMIT;
-- 发现异常则执行：
--   ROLLBACK;
