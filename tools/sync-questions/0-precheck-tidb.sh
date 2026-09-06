#!/usr/bin/env bash
# ============================================================
# TiDB 环境预检
#
# 线上是 TiDB（不是 MySQL），有几处行为差异会影响本次同步。
# 正式跑同步前先执行本脚本，确认这些差异的实际影响。
# ============================================================
set -euo pipefail

MYSQL_BIN="/c/Program Files/MySQL/MySQL Server 8.0/bin"
: "${ONLINE_HOST:?请设置 ONLINE_HOST}"
: "${ONLINE_PORT:=4000}"   # TiDB Cloud 默认端口是 4000，不是 3306
: "${ONLINE_USER:?请设置 ONLINE_USER}"
: "${ONLINE_PASS:?请设置 ONLINE_PASS}"
: "${ONLINE_DB:=test}"

# TiDB Cloud Serverless 强制 TLS，不加 --ssl-mode 会直接连不上
SSL_OPT="${SSL_OPT:---ssl-mode=REQUIRED}"

M=("$MYSQL_BIN/mysql.exe" -h"$ONLINE_HOST" -P"$ONLINE_PORT"
   -u"$ONLINE_USER" -p"$ONLINE_PASS" --default-character-set=utf8mb4
   $SSL_OPT "$ONLINE_DB")

echo "==> 1. 确认是 TiDB 及其版本"
"${M[@]}" -t -e "SELECT VERSION() AS 版本, DATABASE() AS 当前库, TIDB_VERSION() AS TiDB详情;" \
  || echo "（TIDB_VERSION() 不可用，可能不是 TiDB）"

echo "==> 2. 外键是否真正生效"
# TiDB 6.6 以下会【解析但忽略】外键约束：建表不报错，约束却不存在。
# 影响：本方案第2步靠 map_user 保证 created_by 有效，不依赖外键报错，
#       所以外键失效不会导致数据错乱；但也意味着写坏了不会被数据库拦住。
"${M[@]}" -t -e "
SELECT @@foreign_key_checks AS 外键检查开关;
SELECT COUNT(*) AS questions上的外键数
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'questions'
  AND REFERENCED_TABLE_NAME IS NOT NULL;"

echo "==> 3. 事务大小限制"
# TiDB 单事务有大小上限（txn-total-size-limit，默认 100MB）。
# 题库含 LaTeX 长文本 + MEDIUMTEXT 图片字段，题量大时可能触顶。
"${M[@]}" -t -e "SHOW VARIABLES LIKE 'tidb_mem_quota_query';" 2>/dev/null || true

echo "==> 4. 目标表现状"
"${M[@]}" -t -e "
SELECT 'questions' AS 表名, COUNT(*) AS 行数 FROM questions
UNION ALL SELECT 'question_options', COUNT(*) FROM question_options
UNION ALL SELECT 'solution_steps',   COUNT(*) FROM solution_steps
UNION ALL SELECT 'knowledge_tags',   COUNT(*) FROM knowledge_tags
UNION ALL SELECT 'question_shares',  COUNT(*) FROM question_shares
UNION ALL SELECT 'users',            COUNT(*) FROM users;"

echo ""
echo "预检通过。若上面版本确认是 TiDB，请在跑 4-load-online.sh 时"
echo "同样带上 ONLINE_PORT=4000 和 SSL_OPT=--ssl-mode=REQUIRED。"
