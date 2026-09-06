#!/usr/bin/env bash
# ============================================================
# 步骤1b：导出本地 users 的 (id, username)
#
# 不导入 users 表本身 —— 线上用户是真实账号，绝不能被本地账号覆盖。
# 这里只取一份 (本地id, username) 清单，到线上按 username 对齐，
# 生成 map_user。questions.created_by 和 question_shares.user_id
# 都要靠它翻译成线上的 user id。
# ============================================================
set -euo pipefail

CONTAINER="${CONTAINER:-aiproject-mysql}"
LOCAL_USER="${LOCAL_USER:-root}"
LOCAL_PASS="${LOCAL_PASS:-root}"
LOCAL_DB="${LOCAL_DB:-aiproject}"   # 本地实际在跑的库（本地不做任何改动）

docker exec -i "$CONTAINER" mysql \
  -u"$LOCAL_USER" -p"$LOCAL_PASS" \
  --default-character-set=utf8mb4 -N -B "$LOCAL_DB" \
  -e "SELECT CONCAT('(', id, \",'\", REPLACE(username, \"'\", \"''\"), \"'),\") FROM users ORDER BY id" \
  2>/dev/null | tr -d '\r' > users-rows.tmp

{
  echo "INSERT INTO map_user (local_id, username) VALUES"
  sed '$ s/,$/;/' users-rows.tmp
} > local-users-map.sql

rm -f users-rows.tmp
echo "用户映射导出 -> local-users-map.sql"
cat local-users-map.sql
