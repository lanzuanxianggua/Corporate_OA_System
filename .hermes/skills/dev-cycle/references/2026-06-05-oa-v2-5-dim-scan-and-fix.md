# OA v2 5 维度扫描 + 修复实战 (2026-06-05)

## 任务

用户："先修复当前项目存在的问题，再开始下一步的开发"

## 5 维度扫描结果

| 维度 | 工具/方法 | 发现 |
|---|---|---|
| 1. 代码 | `mvn -pl oa-platform-common,oa-platform-security,oa-system,oa-platform-web,oa-workflow -am -DskipTests compile` + 看 lombok/import/setter | 5/5 编译 PASS ✓; lombok 0 import (业务代码手写 getter/setter) ✓; BizException 4 构造器 (RCode,String 兼容) ✓; UserContext 用 get() ✓; setDelFlag("0") 已修 ✓ |
| 2. SQL | 读 V100/V200/V900, DESCRIBE 业务表 | 12+8 表 AUTO_INCREMENT ✓; V900 SELECT 1 修过 ✓; **🔴 V200 wf_*.del_flag TINYINT(1) ↔ entity String 类型不匹配**; **⚠️ sql/ 归档副本漂移** |
| 3. POM | 父 pom plugin + 子 pom 依赖继承 | 13 业务模块 lombok ✓; spring-boot-maven-plugin 在父 pom ✓; **⚠️ 平台层 lombok 不一致 (common/workflow 有, security/web/system 无)** |
| 4. 资源/配置 | 读 application*.yml | 4 套 profile ✓; test profile H2 + disable Flyway ✓; prod env vars ✓; **⚠️ 默认 username/password 是 oa_v2/oa_v2 硬编码** |
| 5. Git/状态 | git status + worktree list | 9 commit ✓; 20+ worktree 孤儿 (2 locked) → 全部 prune ✓ |

## 6 个 Bug 分类

P0（阻塞 e2e）：
- P0 #1 V200 wf_* del_flag TINYINT(1) ↔ entity String 不匹配
- P0 #2 application.yml 默认密码硬编码

P1（代码质量）：
- P1 #1 sql/ 归档副本漂移
- P1 memory 3 条错记（lombok/BizException/AuthException）

P2（清理）：
- P2 #1 平台层 lombok 依赖不一致
- P2 #2 worktree 孤儿 20+ locked

## 修复链

P0：
1. V200 TINYINT(1) → CHAR(1) 4 处
2. application.yml password 改空 + url localhost → 127.0.0.1

P1：
3. git rm code/backend/sql/ 2 文件（双副本陷阱根治）
4. memory 重写，11 条硬规则更新

P2：
5. 3 模块 (security/web/system) 加 lombok 依赖
6. 20 worktree 全部 unlock + prune + 删 .claude/worktrees/

mvn 5 模块 compile + test 全过 (5/5 BUILD SUCCESS)

## e2e 启动卡循环（7 次重启）

| 次数 | 错 | 修 |
|---|---|---|
| 1 | spring-boot:run No plugin found | 父 pom 加 spring-boot-maven-plugin |
| 2 | Druid user_variables_by_thread 权限不够 | GRANT ALL PRIVILEGES WITH GRANT OPTION |
| 3 | V900 Unknown column 'id' (web/resources 副本没改) | 双副本同步 |
| 4 | V200 Table already exists (history 有 V200 success=0) | DELETE V200 history + DROP wf_* 8 表 |
| 5 | V200 wf_definitions already exists (单数/复数错) | DROP 复数 wf_definitions 8 表 |
| 6 | V200 23ms 失败 (history success=0 仍存在) | UPDATE success=1 + DROP wf_* |
| 7 | **Started OaSystemApplication in 5.359s** ✓ | -- |

## 双副本陷阱（开发时容易忘）

**Flyway 实际跑的不是 `code/backend/sql/v2/migration/`，是 `oa-platform-web/src/main/resources/db/migration/`**（classpath 决定）。

sibling 历史上把 V100/V900/V200 同时放两个地方：
- `code/backend/sql/v2/migration/`（归档目录，人读）
- `code/backend/oa-platform-web/src/main/resources/db/migration/`（Flyway classpath）

**只改归档目录 = 改空气**。验证 Flyway 实际用的 V：

```bash
cat oa-platform-web/target/classes/db/migration/V900__init_seed.sql | tail -10
```

**根治**：删除归档目录 `git rm code/backend/sql/`，所有 SQL 改动只走 web/resources。

## V200 wf_* 表名复数陷阱

V200 第一行 `CREATE TABLE wf_definitions` 是**复数**（不是 `wf_definition` 单数）。
DROP 时漏掉单复数差异 → 部分表残留 → V200 重跑时 "Table already exists"。

**教训**：看 V SQL 确认所有 CREATE TABLE 名，DROP 时精确列。

## `mvn compile` 不重复制 resource 的诊断

spring-boot:run 用 `target/classes/application.yml` 和 `target/classes/db/migration/V*.sql`。
改 src 后必须：

```bash
mvn -pl oa-platform-web -am -DskipTests compile
```

**判断**：boot.log 第 1 个 "create connection SQLException" 的 url，如果还是 localhost 不是 127.0.0.1 → mvn 没重编译。
