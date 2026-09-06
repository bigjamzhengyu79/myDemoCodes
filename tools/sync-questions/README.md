# 题库同步到线上 mathedu

**目标：把线上数据库从 `test` 切换为 `mathedu`。本地不做任何修改。**

做法是先把空的线上 `mathedu` 建好（表结构 + 基础账号 + 190 道题），
确认无误后再改 Render 环境变量，把后端从 `test` 切过去。

## 环境实况（已实地核实，与仓库内注释不符）

| | 本地 | 线上 |
|---|---|---|
| 数据库 | MySQL 8.0.45（Docker 容器 `aiproject-mysql`） | TiDB |
| 当前在跑 | **`aiproject`**（保持不动） | **`test`**（切换前不动） |
| 本次动作 | 只读导出，不改任何东西 | 建起 `mathedu` 并切过去 |
| 连接 | `docker exec`，宿主机 MySQL 服务是停止的 | 端口 `4000` + 强制 TLS |

几处已核实的事实：

- **本地实际在跑的是 `aiproject`**：活动连接查询显示 HikariCP 的 2 条连接
  都挂在 `aiproject` 上。虽然 `application.properties` 的默认值写的是 `mathedu`，
  但运行中的进程早于该配置启动，且本次不重启、不改本地配置。
  导出脚本因此统一取 `aiproject`。
- 本地容器里另有 `mathedu` 和 `aiProject2`。`mathedu` 与 `aiproject`
  **数据完全一致**（16 张表 `CHECKSUM TABLE` 逐一相同），是改名时留下的副本。
  取哪个导出结果都一样，取 `aiproject` 是因为它才是实际在服务的库。
- 线上 `test` 有数据、`mathedu` 是空库。提交 `0893a84` 声称
  「线上已按本地结构重建」，实际并未切换过去。
- **仓库的 `schema.sql` 只有 17 张表，实际库有 26 张。**
  差的 9 张是 Hibernate `ddl-auto=update` 自动建的，从未回写。
  所以建表用 `2a-export-schema.sh` 从实际库导出，不用 `schema.sql`。

## 为什么不能直接 mysqldump 导入

即便目标是空库，子表的 `question_id` 指向的仍是**本地 ID**。
线上 `users` 是重新建的，`teacher01` 拿到的 ID 不一定是 1。
直接导入会让 `created_by` 指向错误的用户，或因外键失败。

所以仍走**暂存表 + ID 重映射**：本地数据先原样灌进 `stg_*`，
再在线上库内部把本地 ID 翻译成线上 ID，然后写入正式表。

## 涉及的表

| 表 | 处理方式 |
|---|---|
| `questions` | 重新分配 ID，映射记录在 `map_question` |
| `question_options` / `solution_steps` | 跟随父题的新 ID |
| `knowledge_tags` | 按 `name` 去重复用 |
| `question_knowledge_tags` / `question_shares` | 两端 ID 都翻译 |
| `users` | 只建 `admin` / `teacher01`，按 `username` 对齐 |

## TiDB 差异（脚本已处理）

| 项 | 值 |
|---|---|
| `ONLINE_PORT` | `4000`（不是 3306） |
| `SSL_OPT` | `--ssl-mode=REQUIRED`（Serverless 强制 TLS） |
| `ONLINE_DB` | `mathedu` |

- **排序规则** —— 本地是 MySQL 8.0 专有的 `utf8mb4_0900_ai_ci`，TiDB 不支持，
  建表会报 `Unsupported collation`。导出脚本自动替换为 `utf8mb4_general_ci`。
- **`AUTO_INCREMENT` 不连续、不保证单调** —— TiDB 按节点分配 ID 区间。
  所以不用 `LAST_INSERT_ID()`，而靠 `(作者, 题干)` 回查映射。
- **外键可能被忽略** —— TiDB 6.6 以下解析但不执行。
  引用完整性由 `map_*` 自己保证，不受影响。
- **事务大小上限 100MB** —— 实测导出 8.5MB，余量充足。

## 执行顺序

```bash
cd tools/sync-questions

# 0. TiDB 预检（确认版本、TLS、目标库现状）
ONLINE_HOST=gateway01.xxx.tidbcloud.com ONLINE_USER=xxx ONLINE_PASS=xxx \
./0-precheck-tidb.sh

# 1. 本地导出（容器 aiproject-mysql 需在运行）
./2a-export-schema.sh      # 26 张表结构
./1-export-local.sh        # 题库数据
./1b-export-users.sh       # 用户名映射

# 2. 演练：建表 → 建账号 → 合并 → 自动回滚
ONLINE_HOST=... ONLINE_USER=... ONLINE_PASS=... ./4-load-online.sh

# 3. 核对无误后真正写入
MODE=commit ONLINE_HOST=... ONLINE_USER=... ONLINE_PASS=... ./4-load-online.sh
```

演练模式跑完自动 `ROLLBACK`，只有建表和账号会留下（DDL 无法回滚）。

## 必须盯的三个卡点

合并脚本中间有三条 `SELECT`，**都必须是空结果**：

0. **题干重复检查** —— 同作者下题干完全相同的题。
   映射靠 `(作者, 题干)` 回查，重复会导致选项挂错题。
   **本地实测：0 条，通过。**
1. **未在线上找到的用户** —— 本地作者在线上不存在。
   **本地实测：190 道题全部出自 `teacher01` 一人**，
   所以只要 `2b-seed-users.sql` 建好它就能过。
2. **未映射的本地题目ID** —— 通常是卡点1的连带后果。

## 图片：不需要传文件

**已实测**：题目插图以 base64 data URI 直接存在字段里——

```
["data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."]
```

全库**零条**路径型引用（`6-check-images.sh` 可复核）。
图片随数据库一起走，`backend/uploads/` 那 11 个文件属于另一套
上传通道（附件/教材），与题目插图无关。

体积：`solution_steps` 7.43MB + `questions` 0.84MB = 导出文件 8.5MB。

## ✅ 导入已完成（2026-08-19）

线上 `mathedu` 已写入完毕，实测结果：

| 项 | 线上 mathedu | 本地对照 |
|---|---|---|
| questions | **190** | 190 |
| question_options | **984** | 984 |
| solution_steps | **453** | 453 |
| 含图步骤 / 体积 | **137 / 7.43MB** | 137 / 7.43MB |
| users | admin, teacher01 | — |

- 三个卡点全部为空（题干无重复、出题人已对齐、题目全部映射）
- 引用完整性核查：孤儿题目 / 孤儿选项 / 孤儿步骤 **均为 0**
- 日文 LaTeX 内容抽查正常（`$\textbf{整式の整理}$` 等）

**线上 `test` 全程未被触碰**，后端目前仍连着它。

### 导入过程中修掉的三个问题

1. **`users.created_at` 无默认值** —— 线上表是 Hibernate 建的，
   该列 NOT NULL 且无 DEFAULT，TiDB 不像 MySQL 那样用零值兜底，
   不显式写入会报 `Field 'created_at' doesn't have a default value`。
2. **排序规则冲突** —— 线上正式表是 `utf8mb4_0900_ai_ci`，
   暂存表原本写的是 `utf8mb4_unicode_ci`，JOIN 时报
   `Illegal mix of collations`。已统一为 `0900_ai_ci`，
   并把建暂存表改成 DROP + CREATE（否则改了 DDL 也不生效）。
3. **卡点1 过严** —— 原本检查「所有本地用户」，会因 student01~06、
   teacher02 在线上不存在而误报。实际只有出题人重要，
   已改为只检查 `stg_questions.created_by` 引用到的账号。

## 切换（最后一步，需要你在 Render 面板操作）

题库已导完并核对无误，现在改 Render 后端的环境变量：

```
SPRING_DATASOURCE_URL=jdbc:mysql://gateway01.xxx.tidbcloud.com:4000/mathedu?sslMode=VERIFY_IDENTITY&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8
SPRING_DATASOURCE_USERNAME=<集群前缀>.root
SPRING_DATASOURCE_PASSWORD=<密码>
```

只需把库名从 `test` 改成 `mathedu`，其余连接参数不变。

⚠️ **先检查 `DB_HOST`**：[application.properties](../../backend/src/main/resources/application.properties#L23)
里 `DB_HOST` 被当作**完整 JDBC URL** 使用（不是主机名），
且优先级**高于** `SPRING_DATASOURCE_URL`。
若面板里还留着 `DB_HOST` 且指向 test，改 `SPRING_DATASOURCE_URL` 不会生效。
**切换前必须确认 `DB_HOST` 已删除，或已改成上面那串完整的 mathedu URL。**

改完重启后端，然后确认：
1. 能用 `teacher01 / 123456` 登录
2. 题库列表有 190 道题
3. 题目里的图片正常显示（base64 内嵌，不依赖文件）

### ⚠️ 切换后会「丢失」的数据

`mathedu` 目前只有 **admin / teacher01 + 190 道题**。
`test` 里的这些数据**没有迁过去**：

| 数据 | test 里的量 |
|---|---|
| 学生账号 | 4 个（student01~04） |
| 班级 | 2 个 |
| 作业 | 2 份 |
| 学生作答 | 2 条 |
| 原有题目 | 5 道 |

切过去之后这部分在页面上会消失。按你说的「之后导什么数据再看情况」，
先保持现状。**在决定之前，`test` 库请勿删除。**

回滚很简单：把 `SPRING_DATASOURCE_URL` 的库名改回 `test` 并重启即可。


## 密码是明文

`2b-seed-users.sql` 写的是明文 `123456`，与本地一致。这不是疏忽：
[AuthService.login()](../../backend/src/main/java/com/example/homework/service/AuthService.java#L26)
用 `Objects.equals(明文, user.getPassword())` 直接比对，
注入的 `passwordEncoder` **从未被调用**。写 bcrypt 哈希反而登录不了。

这是既有的安全问题（明文存储 + 弱口令），修它属于另一件事。
**上线前请务必改口令。**

## 幂等性

整套脚本可重复执行：`stg_*` / `map_*` 每次先 `TRUNCATE`；
选项与解题步骤先 `DELETE` 后 `INSERT`，作用域被 `map_question` 限死；
关联表用 `INSERT IGNORE`；建表与建账号都做存在性判断。

## 回滚

切换前，线上生产数据始终在 `test` 里，本流程全程不碰它。
`mathedu` 弄坏了直接 `DROP DATABASE mathedu` 重来即可，用户无感知。

## 收尾

线上观察几天确认无误后，跑 `5-cleanup.sql` 删掉 `stg_*` 和 `map_*`。
不必急 —— `map_question` 保留着本地题 ID 到线上题 ID 的对应关系，
万一发现某道题导错了，靠它才能精确定位。
