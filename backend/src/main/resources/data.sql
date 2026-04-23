-- 插入种子用户（密码都是 bcrypt 加密密码：123456）
INSERT IGNORE INTO users (username, password, real_name, role, class_name)
VALUES
    ('teacher01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', '张老师', 'TEACHER', '高三(1)班');

-- 插入示例数据到 goals 表，含父目标和子目标
INSERT IGNORE INTO goals (id, title, description, status, planned_start, planned_end, actual_start, actual_end, progress, owners, parent_id, created_at, updated_at)
VALUES
  (1, '机器学习能力体系建设', '从基础理论到工程落地的完整学习路径', 'IN_PROGRESS', '2025-01-01', '2025-06-30', '2025-01-06', NULL, 0, '张伟,李娜', NULL, NOW(), NOW()),
  (2, '云原生与 DevOps 能力提升', '构建从容器化到自动化部署的全链路能力', 'IN_PROGRESS', '2025-02-01', '2025-08-31', '2025-02-01', NULL, 0, '陈强,王芳', NULL, NOW(), NOW()),
  (3, '数据分析与可视化专项', '掌握从数据清洗到业务洞察的完整工作流', 'TODO', '2025-03-01', '2025-07-31', NULL, NULL, 0, '刘洋', NULL, NOW(), NOW()),
  (101, '监督学习基础理论', '线性回归、逻辑回归、决策树核心原理', 'DONE', '2025-01-06', '2025-02-28', '2025-01-06', '2025-02-25', 100, '张伟', 1, NOW(), NOW()),
  (102, '无监督学习与聚类算法', 'K-Means、DBSCAN、PCA 降维技术', 'IN_PROGRESS', '2025-03-01', '2025-04-30', '2025-03-03', NULL, 60, '张伟,李娜', 1, NOW(), NOW()),
  (103, '深度学习与神经网络实战', 'PyTorch 框架下的 CNN / RNN 模型训练', 'TODO', '2025-05-01', '2025-06-30', NULL, NULL, 0, '李娜', 1, NOW(), NOW()),
  (201, 'Docker 与容器化基础', '镜像构建、网络配置与多容器编排', 'LATE', '2025-02-01', '2025-03-31', '2025-02-01', NULL, 80, '陈强', 2, NOW(), NOW()),
  (202, 'Kubernetes 集群管理', 'Pod、Service、Ingress 及 Helm Chart 实践', 'IN_PROGRESS', '2025-04-01', '2025-06-30', '2025-04-07', NULL, 35, '陈强,王芳', 2, NOW(), NOW()),
  (203, 'CI/CD 流水线设计', 'GitHub Actions + ArgoCD 自动化部署实践', 'TODO', '2025-07-01', '2025-08-31', NULL, NULL, 0, '王芳', 2, NOW(), NOW()),
  (301, 'Pandas 数据处理进阶', '多表关联、时间序列、缺失值处理策略', 'TODO', '2025-03-01', '2025-04-30', NULL, NULL, 0, '刘洋', 3, NOW(), NOW()),
  (302, '数据可视化与 BI 报表', 'Matplotlib、Seaborn、Tableau 综合运用', 'TODO', '2025-05-01', '2025-07-31', NULL, NULL, 0, '刘洋', 3, NOW(), NOW());
