#!/usr/bin/env bash
# ============================================================
# 步骤1：从本地库导出 questions 及其从属表到 staging 用的 SQL
#
# 本地 MySQL 跑在 Docker 容器 aiproject-mysql 里，
# 宿主机的 MySQL 服务是停止的 —— 所以走 docker exec，
# 不要用宿主机的 mysqldump.exe。
#
# 导出成 stg_* 前缀的 INSERT，先整体灌进线上库做暂存，
# 再在线上库内部用 SQL 做 ID 重映射合并（步骤3）。
# 不直接导入正式表 —— 线上已有数据，主键会撞。
# ============================================================
set -euo pipefail

CONTAINER="${CONTAINER:-aiproject-mysql}"
LOCAL_USER="${LOCAL_USER:-root}"
LOCAL_PASS="${LOCAL_PASS:-root}"
LOCAL_DB="${LOCAL_DB:-aiproject}"   # 本地实际在跑的库（本地不做任何改动）
OUT="local-questions-dump.sql"

echo "==> 从容器 $CONTAINER 的 $LOCAL_DB 库导出"

# --no-create-info:  不导出建表语句，stg_* 结构由 2-create-staging.sql 定义
# --complete-insert: 带列名，避免列顺序差异导致错位
# --hex-blob:        二进制安全
# 注意 docker exec 不加 -t，避免输出被 TTY 转换污染（会插入 \r）
docker exec -i "$CONTAINER" mysqldump \
  -u"$LOCAL_USER" -p"$LOCAL_PASS" \
  --default-character-set=utf8mb4 \
  --no-create-info \
  --complete-insert \
  --skip-add-locks \
  --skip-disable-keys \
  --skip-set-charset \
  --hex-blob \
  "$LOCAL_DB" \
  questions question_options solution_steps knowledge_tags question_knowledge_tags question_shares \
  2>/dev/null > "$OUT.raw"

# 把 INSERT INTO `questions` 改写成 INSERT INTO `stg_questions`
sed -E \
  -e 's/INSERT INTO `questions`/INSERT INTO `stg_questions`/g' \
  -e 's/INSERT INTO `question_options`/INSERT INTO `stg_question_options`/g' \
  -e 's/INSERT INTO `solution_steps`/INSERT INTO `stg_solution_steps`/g' \
  -e 's/INSERT INTO `knowledge_tags`/INSERT INTO `stg_knowledge_tags`/g' \
  -e 's/INSERT INTO `question_knowledge_tags`/INSERT INTO `stg_question_knowledge_tags`/g' \
  -e 's/INSERT INTO `question_shares`/INSERT INTO `stg_question_shares`/g' \
  "$OUT.raw" > "$OUT"

rm -f "$OUT.raw"
echo "导出完成 -> $OUT"
grep -c "INSERT INTO" "$OUT" | sed 's/^/INSERT 语句数: /'
