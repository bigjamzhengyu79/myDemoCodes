-- 插入示例数据到 math_goals 表
INSERT IGNORE INTO math_goals (id, title, description, category, target_date, progress, status, created_at, updated_at)
VALUES 
    (1, '掌握微积分基础', '学习导数、积分和极限的基本概念', '微积分', '2026-06-30', 45, '进行中', NOW(), NOW()),
    (2, '平面几何证明技巧', '掌握常见的平面几何证明方法和技巧', '几何', '2026-05-31', 60, '进行中', NOW(), NOW()),
    (3, '线性代数入门', '理解向量、矩阵和线性变换的基本概念', '代数', '2026-07-31', 30, '进行中', NOW(), NOW()),
    (4, '概率论基础', '学习基本的概率分布和统计推断', '概率', '2026-08-31', 25, '进行中', NOW(), NOW()),
    (5, '完成高等数学第一章', '完成教科书的第一章所有练习题', '其他', '2026-04-30', 100, '已完成', NOW(), NOW());

-- 插入种子用户（密码都是 bcrypt 加密密码：123456）
INSERT IGNORE INTO users (username, password, real_name, role, class_name)
VALUES
    ('teacher01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '张老师', 'TEACHER', '高三(1)班');
