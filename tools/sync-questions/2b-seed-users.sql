-- ============================================================
-- 步骤2b：向空的线上 mathedu 库写入基础账号
--
-- 本次只建两个账号：admin 与 teacher01。
--   * teacher01 是全部 190 道题的作者（questions.created_by 全指向它），
--     不建它题目就无法归属，合并会在卡点1/2 停下。
--   * admin 是管理后台入口账号。
-- 其余学生账号按需后补 —— 题库本身不依赖它们。
--
-- 【关于密码】
-- 这里写的是明文 123456，与本地库一致。这不是疏忽：
-- AuthService.login() 用的是 Objects.equals(明文, user.getPassword())
-- 直接比对，并未调用注入的 passwordEncoder。
-- 也就是说系统当前就是明文校验 —— 写 bcrypt 哈希反而登录不了。
--
-- 这是既有的安全问题（明文存储 + 弱口令），但修它属于另一件事，
-- 不应混在本次数据同步里。上线前请务必修改口令。
--
-- 【created_at 必须显式写】
-- 线上 users.created_at 是 NOT NULL 且无 DEFAULT（Hibernate 建表所致），
-- 不写会报 Field 'created_at' doesn't have a default value。
-- TiDB 不像 MySQL 那样在宽松模式下用零值兜底。
--
-- id 不写死：让 TiDB 自行分配。
-- 后续 map_user 是按 username 对齐的，不依赖具体 id 值。
-- ============================================================

INSERT INTO users (username, password, real_name, role, created_at, updated_at)
SELECT 'admin', '123456', '系统管理员', 'ADMIN', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO users (username, password, real_name, role, created_at, updated_at)
SELECT 'teacher01', '123456', '张老师', 'TEACHER', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'teacher01');

-- 确认
SELECT id, username, real_name, role FROM users ORDER BY id;
