#!/usr/bin/env bash
# ============================================================
# 步骤6：图片检查
#
# 【结论：本次同步不需要单独传图片文件】
#
# 已实测确认：题目图片是以 base64 data URI 的形式
# 直接存在 image_urls_json 字段里的，例如
#   ["data:image/png;base64,iVBORw0KGgo..."]
# 而不是 "/uploads/xxx.png" 这种文件路径。
#
# 所以图片随数据库一起走，导出的 SQL 里已经包含了全部图片数据。
# backend/uploads/ 下那些文件是【另一套】上传通道（附件/教材），
# 与 questions / solution_steps 的插图无关。
#
# 代价是体积：solution_steps 的图片字段占 7.43MB，
# questions 占 0.84MB —— 这是导出文件有 8.5MB 的原因，
# 也是 TiDB 事务大小需要留意的原因（见 README）。
#
# 本脚本保留下来只为复核：确认没有遗漏的路径型引用。
# ============================================================
set -euo pipefail

CONTAINER="${CONTAINER:-aiproject-mysql}"
LOCAL_DB="${LOCAL_DB:-aiproject}"   # 本地实际在跑的库（本地不做任何改动）

echo "==> 检查是否存在【路径型】图片引用（这类才需要传文件）"
docker exec -i "$CONTAINER" mysql -uroot -proot --default-character-set=utf8mb4 -t "$LOCAL_DB" -e "
SELECT 'questions' AS src, id, LEFT(image_urls_json, 60) AS sample
FROM questions
WHERE image_urls_json LIKE '%/uploads/%'
UNION ALL
SELECT 'solution_steps', question_id, LEFT(image_urls_json, 60)
FROM solution_steps
WHERE image_urls_json LIKE '%/uploads/%';" 2>/dev/null

echo ""
echo "上面若为空 => 全部是内嵌 base64，不需要传任何文件。"
echo ""
echo "==> 图片数据体积（影响 TiDB 事务大小）"
docker exec -i "$CONTAINER" mysql -uroot -proot -t "$LOCAL_DB" -e "
SELECT 'questions' AS tbl,
       ROUND(SUM(LENGTH(IFNULL(image_urls_json,'')))/1024/1024,2) AS img_mb
FROM questions
UNION ALL
SELECT 'solution_steps',
       ROUND(SUM(LENGTH(IFNULL(image_urls_json,'')))/1024/1024,2)
FROM solution_steps;" 2>/dev/null
