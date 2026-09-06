#!/usr/bin/env bash
# ============================================================
# 步骤2a：导出本地完整表结构，用于在空的线上 mathedu 建表
#
# 为什么不用仓库里的 schema.sql：
#   schema.sql 只有 17 张表，而实际库里有 26 张 ——
#   差的 9 张（goal_instances / math_goals / teacher_class_groups 等）
#   是 Hibernate ddl-auto=update 自动建的，从未回写到 schema.sql。
#   照 schema.sql 建库，后端一启动就会因缺表而报错或再次自动建表，
#   且自动建出来的定义与本地不一致（V21/V22 注释里已有教训）。
#
# 所以直接从本地实际库导出结构，保证两端完全一致。
#
# TiDB 注意：
#   导出的 DDL 含 ENGINE=InnoDB（TiDB 会忽略，无害）与外键定义。
#   TiDB 6.6 以下会解析但不执行外键 —— 不影响本方案，
#   因为引用完整性由 map_* 表在合并时自己保证。
# ============================================================
set -euo pipefail

CONTAINER="${CONTAINER:-aiproject-mysql}"
LOCAL_USER="${LOCAL_USER:-root}"
LOCAL_PASS="${LOCAL_PASS:-root}"
LOCAL_DB="${LOCAL_DB:-aiproject}"   # 本地实际在跑的库（本地不做任何改动）
OUT="local-schema.sql"

echo "==> 从容器 $CONTAINER 的 $LOCAL_DB 导出表结构（不含数据）"

docker exec -i "$CONTAINER" mysqldump \
  -u"$LOCAL_USER" -p"$LOCAL_PASS" \
  --default-character-set=utf8mb4 \
  --no-data \
  --skip-add-drop-table \
  --skip-set-charset \
  "$LOCAL_DB" \
  2>/dev/null | tr -d '\r' > "$OUT"

# AUTO_INCREMENT=xxx 起始值不必带到线上，去掉更干净
sed -i -E 's/ AUTO_INCREMENT=[0-9]+//g' "$OUT"

echo "导出完成 -> $OUT"
echo -n "表数量: "; grep -c "^CREATE TABLE" "$OUT"

# ------------------------------------------------------------
# TiDB 兼容处理：utf8mb4_0900_ai_ci 是 MySQL 8.0 专有排序规则，
# TiDB 不支持，建表会直接报 Unsupported collation。
# 换成 utf8mb4_general_ci（TiDB 默认支持，且与本地行为足够接近）。
#
# 注意：不改成 utf8mb4_unicode_ci —— 仓库 schema.sql 虽然写的是它，
# 但实际库是 0900_ai_ci，两者排序规则不同。选 general_ci 是因为
# TiDB 对它支持最稳妥；题库场景只做等值比较，排序差异无实际影响。
# ------------------------------------------------------------
sed -i 's/utf8mb4_0900_ai_ci/utf8mb4_general_ci/g' "$OUT"
echo -n "已替换 0900_ai_ci -> general_ci，剩余不兼容排序规则: "
grep -c "0900_ai_ci" "$OUT" || true
