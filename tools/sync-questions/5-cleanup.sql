-- ============================================================
-- 步骤5：确认线上一切正常后，清理暂存表
--
-- 建议先别急着跑 —— map_question 保留着「本地题ID -> 线上题ID」，
-- 万一发现某道题导错了，靠它才能精确定位。
-- 观察几天没问题再清理。
-- ============================================================
DROP TABLE IF EXISTS stg_question_shares;
DROP TABLE IF EXISTS stg_question_knowledge_tags;
DROP TABLE IF EXISTS stg_solution_steps;
DROP TABLE IF EXISTS stg_question_options;
DROP TABLE IF EXISTS stg_questions;
DROP TABLE IF EXISTS stg_knowledge_tags;

-- 映射表最后再删
DROP TABLE IF EXISTS map_question;
DROP TABLE IF EXISTS map_tag;
DROP TABLE IF EXISTS map_user;
