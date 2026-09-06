#!/usr/bin/env bash
# ============================================================
# 步骤4：在线上空库 mathedu 中建表 -> 灌基础账号 -> 合并题库
#
# 本次目标是【空的 mathedu】，不是正在跑的 test：
#   1. 用本地导出的结构建全部 26 张表
#   2. 写入 admin / teacher01 两个账号
#   3. 灌暂存表并把 190 道题合并进去
#   4. 确认无误后，再改 Render 环境变量把后端切到 mathedu
#
# 切换是最后一步，且由你手动做 —— 本脚本只负责把 mathedu 准备好。
# 在切换之前，线上跑的仍是 test，用户无感知，随时可以放弃重来。
#
# 用法：
#   ONLINE_HOST=gateway01.xxx.tidbcloud.com ONLINE_USER=xxx ONLINE_PASS=xxx \
#   ./4-load-online.sh              # 演练，跑完自动回滚
#   MODE=commit ... ./4-load-online.sh   # 真正写入
# ============================================================
set -euo pipefail

MYSQL_BIN="/c/Program Files/MySQL/MySQL Server 8.0/bin"
: "${ONLINE_HOST:?请设置 ONLINE_HOST}"
: "${ONLINE_PORT:=4000}"   # TiDB Cloud 默认 4000，不是 MySQL 的 3306
: "${ONLINE_USER:?请设置 ONLINE_USER}"
: "${ONLINE_PASS:?请设置 ONLINE_PASS}"
: "${ONLINE_DB:=mathedu}"  # 本次写入目标是空的 mathedu

# TiDB Cloud Serverless 强制 TLS，不带 --ssl-mode 会直接连不上。
# 自建 TiDB 若未开 TLS，传 SSL_OPT= 置空即可。
SSL_OPT="${SSL_OPT:---ssl-mode=REQUIRED}"
# 不加 -f/--force：mysql 默认遇到 SQL 错误就停止并返回非 0。
# 这点很关键——合并脚本中途报错必须中断，否则后续语句会在
# 映射不完整的状态下继续跑，产生半截数据。
# （注意 8.0.21 没有 --abort-source-on-error 选项，默认行为已经够用。）
M=("$MYSQL_BIN/mysql.exe" -h"$ONLINE_HOST" -P"$ONLINE_PORT"
   -u"$ONLINE_USER" -p"$ONLINE_PASS" --default-character-set=utf8mb4
   $SSL_OPT "$ONLINE_DB")

echo "==> 0. 连接测试"
"${M[@]}" -t -e "SELECT VERSION() AS db_version, DATABASE() AS current_db;"

# ------------------------------------------------------------
# 防呆：本次目标是【空的 mathedu】，不是正在服务的 test。
# 若误把 ONLINE_DB 指向 test，会把题库灌进生产库 —— 必须拦住。
# ------------------------------------------------------------
if [ "$ONLINE_DB" = "test" ]; then
  echo "!! ONLINE_DB=test 是线上正在服务的库，本次不应写入。"
  echo "!! 本次目标是空的 mathedu。确认要写 test 请加 ALLOW_TEST=1。"
  [ "${ALLOW_TEST:-0}" = "1" ] || exit 1
fi

echo "==> 1. 建表（26 张，来自本地实际结构）"
# 空库首次执行才需要建表；已建过则 CREATE TABLE IF NOT EXISTS 式跳过。
# local-schema.sql 由 2a-export-schema.sh 生成，已做 TiDB 排序规则替换。
if [ ! -f local-schema.sql ]; then
  echo "!! 缺少 local-schema.sql，请先跑 ./2a-export-schema.sh"
  exit 1
fi
EXISTING_TABLES=$("${M[@]}" -N -B -e "
  SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$ONLINE_DB';")
echo "目标库现有 ${EXISTING_TABLES} 张表"

if [ "$EXISTING_TABLES" -lt 26 ]; then
  echo "   -> 执行建表"
  "${M[@]}" < local-schema.sql
  "${M[@]}" -t -e "
    SELECT COUNT(*) AS tables_after FROM information_schema.tables
    WHERE table_schema='$ONLINE_DB';"
else
  echo "   -> 表已齐全，跳过"
fi

echo "==> 1b. 写入基础账号 admin / teacher01"
# teacher01 是全部 190 道题的作者，不建它合并会在卡点1/2 停下。
"${M[@]}" -t < 2b-seed-users.sql
# 说明：本次目标是空库 mathedu，没有既有数据需要备份，
# 所以不做 mysqldump（原备份步骤是为写 test 这类生产库准备的）。
# 真正的生产数据仍在 test 库里，本次全程不碰它 —— 这就是最好的回滚保障：
# mathedu 弄坏了直接 DROP 重来即可，切换前用户完全无感知。

echo "==> 2. 建暂存表"
# 先 DROP 再建：暂存表是本流程私有的临时对象，重建无副作用。
# 这样排序规则等定义变更后能立即生效 —— CREATE TABLE IF NOT EXISTS
# 遇到已存在的旧表会静默跳过，导致改了 DDL 却不生效。
"${M[@]}" -e "DROP TABLE IF EXISTS stg_questions, stg_question_options,
              stg_solution_steps, stg_knowledge_tags,
              stg_question_knowledge_tags, stg_question_shares,
              map_question, map_tag, map_user;"
"${M[@]}" < 2-create-staging.sql
# 步骤3（清空暂存表）已并入步骤2 —— 上面改成了 DROP + CREATE，
# 新建出来本就是空表，无需再 TRUNCATE。

echo "==> 4. 灌入本地数据"
"${M[@]}" < local-users-map.sql
"${M[@]}" < local-questions-dump.sql
"${M[@]}" -e "SELECT COUNT(*) AS 暂存题目数 FROM stg_questions;"

# ============================================================
# 演练 / 提交 两种模式
#
# 为什么要把 ROLLBACK / COMMIT 拼进同一个 SQL 文件再送进去：
#   mysql 客户端读到 EOF 会自动提交并断开，
#   分两次调用 mysql 等于分两个连接，第二个连接里 COMMIT 毫无意义
#   —— 事务在第一个连接结束时就已经被提交掉了。
#   所以事务的结束语句必须和合并语句在同一次调用里。
#
# 默认 MODE=dry：跑完立即 ROLLBACK，只看结果不落库。
# 核对无误后再用 MODE=commit 重跑一次。
# ============================================================
MODE="${MODE:-dry}"

if [ "$MODE" = "commit" ]; then
  echo "==> 5. 执行合并【提交模式】—— 本次会真正写入线上库"
  { cat 3-merge.sql; echo "COMMIT;"; } > .merge-run.sql
else
  echo "==> 5. 执行合并【演练模式】—— 跑完自动回滚，不会写入线上库"
  { cat 3-merge.sql; echo "ROLLBACK;"; } > .merge-run.sql
fi

"${M[@]}" -t < .merge-run.sql
rm -f .merge-run.sql

echo ""
if [ "$MODE" = "commit" ]; then
  echo "############################################################"
  echo "# 已提交。请到应用里确认题库显示正常。"
  echo "# 如需回滚，用步骤1生成的 online-backup-*.sql 恢复。"
  echo "############################################################"
  echo "==> 6. 提交后复核"
  "${M[@]}" -t -e "SELECT COUNT(*) AS 线上题目总数 FROM questions;"
else
  echo "############################################################"
  echo "# 演练完成，线上库未被修改（已 ROLLBACK）。"
  echo "# 请检查上面两个卡点是否为空、第6步数字是否合理。"
  echo "# 确认无误后重跑并提交："
  echo "#   MODE=commit ./4-load-online.sh"
  echo "############################################################"
fi
