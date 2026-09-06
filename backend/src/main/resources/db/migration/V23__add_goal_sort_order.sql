-- goals 表增加 sort_order（同层显式排序位次）
--
-- 背景：此前同层子目标按 planned_start ASC 排序。复制目标时所有日期被清空为
-- NULL，MySQL 的 NULL-first 规则使同层全部并列，顺序由数据库偶然决定；
-- 用户逐个填写并保存后，节点因 planned_start 从 NULL 变为有值而被挤到末尾。
-- 改为显式排序字段后，同层顺序不再受日期编辑影响。
ALTER TABLE goals ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

-- 为已有数据回填初始位次：同层内沿用原先的 planned_start 升序（NULL 在前），
-- 同值时按 id 升序保持稳定，使升级前后的显示顺序尽量一致。
-- IFNULL(parent_id, 0) 让根目标（parent_id 为 NULL）归入同一分组一起编号。
SET @rn := 0;
SET @grp := NULL;
UPDATE goals g
JOIN (
    SELECT id,
           @rn := IF(@grp = IFNULL(parent_id, 0), @rn + 1, 0) AS rn,
           @grp := IFNULL(parent_id, 0) AS grp
    FROM (
        SELECT id, parent_id
        FROM goals
        ORDER BY IFNULL(parent_id, 0),
                 planned_start IS NULL DESC,
                 planned_start,
                 id
    ) ordered
) seq ON seq.id = g.id
SET g.sort_order = seq.rn;

CREATE INDEX idx_goal_parent_sort ON goals(parent_id, sort_order);
