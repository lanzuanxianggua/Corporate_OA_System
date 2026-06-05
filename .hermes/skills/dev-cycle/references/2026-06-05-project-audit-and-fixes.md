# v2 session log: 5 维扫描 + 修复实战 (2026-06-05)

Concrete evidence captured during the "5 维度扫描项目" session
that closed the v2 24h 闭环开发的尾巴。Read this when
doing a project-wide bug audit, when entity/DDL 字段类型不匹配
造成 MyBatis 隐式转换, or when deciding whether to keep
both sql/ 归档目录 and classpath 副本.

## 1. 5 维度扫描方法（v2 项目专用）

每轮扫描 18 模块覆盖 5 维（**实际发现值得扫 8 维**）：

1. **DDL/Entity 一致性** — `del_flag` `is_deleted` `version` 字段类型
   是否 entity field ↔ SQL DDL 严格匹配
2. **依赖一致性** — 父 pom 配的 dependencyManagement 是否所有子 pom
   都能拿到（lombok / spring-boot-starter / jackson 等）
3. **资源/配置双副本** — `sql/v2/migration/` vs `web/resources/db/migration/`
4. **业务空模块识别** — 有 `pom.xml` 但无 java 源 vs 有 java 但只占位
5. **git 工作区卫生** — 孤儿 worktree / 锁死分支 / untracked 大文件
6. **memory 错记审计** — 历史结论（如 "lombok forbidden"）现在还成立吗
7. **CI/surefire flag** — `-Dsurefire.failIfNoSpecifiedTests=false` 而非短名
8. **classpath 优先级** — sibling 改 `AutoConfiguration.imports` 与
   主类 `@Import` 谁先

**扫描命令模板**：

```bash
# 1. DDL 字段类型审计
grep -rn "del_flag\|delFlag" code/backend/ --include="*.java" --include="*.sql" \
  | head -50

# 2. 依赖缺失审计（父 pom 有 <dependencyManagement> + 实际 <dependency> 引用）
for pom in $(find code/backend -name pom.xml -not -path "*/target/*"); do
  echo "=== $pom ==="
  grep -E "<artifactId>(lombok|spring-boot-starter|jackson|hutool)</artifactId>" "$pom" || true
done

# 3. 双副本检测
find code/backend -path "*/db/migration/V*.sql"
find code/backend -path "*/sql/v2/migration/V*.sql"

# 4. 业务空模块
for m in oa-hr-leave oa-hr-employee oa-finance; do
  count=$(find code/backend/$m/src -name "*.java" 2>/dev/null | wc -l)
  if [ "$count" -lt 5 ]; then echo "$m: $count java files"; fi
done

# 5. worktree 孤儿
git worktree list
git worktree list --porcelain | grep "locked" || true
```

## 2. 5/6 修复实战（本次）

### 2.1 P0 #1 V200 wf_* del_flag TINYINT → CHAR(1)

**症状**（理论）：MyBatis-Plus entity 字段是 `String delFlag`,
但 V200__init_workflow.sql DDL 用 `TINYINT(1)` 或 `TINYINT`。
运行时 MySQL 驱动会把 `"0"/"1"` 字符串自动转 int 0/1 写入，
但反查 entity 时不会自动转 String。**可能导致：`del_flag="0"`
写入成功，读取时 entity.getDelFlag() 是 null（隐式转型丢失）**。

**项目硬规则**（v2 反转旧记）：
- 所有 entity 的 `del_flag` 字段：`String` 类型（不是 Integer）
- 所有 SQL DDL 的 `del_flag` 字段：`CHAR(1)` 字符（不是 TINYINT）
- V100 12 张表已配 CHAR(1)
- V200 wf_* 8 张表**需要从 TINYINT(1) 改成 CHAR(1)**（不 commit 之前
  不能简单 mvn compile + spring-boot:run 跑通，e2e 会报 delFlag
  类型不匹配）

**修法**（V200 DDL 批量改）：
```sql
-- 错
`del_flag` TINYINT(1) DEFAULT 0 COMMENT '0-正常 1-删除',

-- 对
`del_flag` CHAR(1) DEFAULT '0' COMMENT '0-正常 1-删除',
```

注意 V200__init_workflow.sql 8 张表都要改：
`wf_process`, `wf_instance`, `wf_task`, `wf_delegation`,
`wf_form`, `wf_form_field`, `wf_action_log`, `wf_attachment`。

### 2.2 P0 #2 application.yml 默认密码硬编码

**错**（commit 前的版本）：
```yaml
password: oa_v2  # 硬编码, git history 留痕
url: jdbc:mysql://localhost:3306/...  # IPv6 socket 在 Windows 上是 ::1
```

**对**（修复后）：
```yaml
password: ${DB_PASSWORD:}  # 空默认, 强制 dev 显式注入
url: jdbc:mysql://127.0.0.1:3306/...  # TCP/IP, 避免 IPv6 路径
user: ${DB_USER:oa_v2}    # user 也走 env
```

**两条 v2 规则**：
1. **dev 默认 password 必为空** (`${DB_PASSWORD:}`) —— 避免硬编码进 git
2. **dev url 走 127.0.0.1** —— Windows 上 `localhost` 解析成 `::1` (IPv6)，
   可能走另一条 socket 路径，Druid 报 Access denied（mysql 客户端
   走 IPv4，行为不一致）

### 2.3 P1 #1 sql/ 归档副本漂移

**症状**：改 `code/backend/sql/v2/migration/V100.sql` 后
spring-boot:run 启动报"Unknown column 'id'" —— 改的是归档目录，
**Flyway classpath 真相源在 `oa-platform-web/src/main/resources/db/migration/`**。

**修法**（2026-06-05 commit 7d071ea）：
```bash
# 同步 web resources + 归档目录（如果保留归档）
cp code/backend/sql/v2/migration/V*.sql \
   code/backend/oa-platform-web/src/main/resources/db/migration/

# 或者终极清理：删归档目录
git rm -rf code/backend/sql/v2/migration
echo "code/backend/sql/v2/migration/" >> .gitignore
```

**本 session 已 git rm 2 文件**（V100__init_platform.sql + V900__init_seed.sql），
未 commit。**下 session 第一件事是确认 staged D 状态**：
```bash
git status | grep -E "D\s+code/backend/sql"
```

### 2.4 P2 #1 platform lombok 依赖不一致

**症状**：父 pom `oa-system-parent` 的 `<dependencyManagement>`
有 lombok 1.18.34，但子 pom `oa-platform-security` / `oa-platform-web`
/ `oa-system` 缺 `<dependency><groupId>org.projectlombok</groupId></dependency>`。

**修法**（3 子 pom 加）：
```xml
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <scope>provided</scope>
</dependency>
```

`provided` scope 让 lombok 编译期可用，运行时不打进 jar（不污染
最终 docker image）。

### 2.5 P2 #2 worktree 孤儿

**症状**：20+ 锁死 worktree，.claude/worktrees/ 还在但 metadata
指向不存在的目录。

**修法**（验证有效）：
```bash
# 1. 看所有 worktree
git worktree list --porcelain | grep -B1 "locked"

# 2. 强制解锁 + 删除
for wt in $(git worktree list --porcelain | grep "^/c/" -B1 | head -40); do
  git worktree remove --force "$wt" 2>/dev/null || git worktree unlock "$wt"
done

# 3. 物理删 .claude/worktrees/
rm -rf .claude/worktrees/

# 4. prune 修 metadata
git worktree prune --verbose
```

**注意**：`git worktree remove --force` 在 windows 上可能 fail
（目录已物理删除但 metadata 还在），需要先 unlock。

## 3. memory 错记审计（反转 3 条）

memory 里"X 不工作"类断言可能 stale（环境变了 / 之前测错了 /
sibling 当年环境不一样）。本次反