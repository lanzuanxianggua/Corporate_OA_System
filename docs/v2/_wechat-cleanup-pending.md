# 待审清单:v1 残留 / 旧版 vs 新版 交杂排查

**生成时间**: 2026-06-08
**分支**: `ai/wechat-001` (与 `v2-platform` HEAD 完全一致,无 divergence)
**目的**: 排查"新旧版本代码交杂"——但经 git 历史回溯,**v1 已在 commit `99f517b`(2026-06-04)被全量删除 966 个文件**,git 历史中已无 v1 残留。
当前"交杂"**实际是 v2 内部**的两类问题:
1. v2 重构/收口期产生的**未提交工作区改动**(65 个 untracked + 63 个 modified + 3 个已删)
2. v2 内部**两套 API 风格并存**: 旧扁平 `/api/xxx` (LegacyApiController) vs v2 命名约定 `/api/v1/{module}/...`

> ⚠️ **会话输入通道**: 你在本 Claude Code 会话里直接回复即可,无需切到微信。分支名 `ai/wechat-001` 只是历史命名,与"必须用 wechat 操作"无关。
> 
> 本清单不执行任何删除,等你逐条勾选后在本会话回复指令。

---

## A. 真·"旧版"删除候选 (大概率可删,等确认)

| # | 路径 | 类别 | 说明 | 推荐动作 |
|---|------|------|------|----------|
| A1 | `MintToken.java` (根目录) | 调测工具 | 本地实验文件,**从未进过 git**,与项目无关,疑似调试 JWT 用 | **删** (仅物理) |
| A2 | `code/backend/oa-platform-web/src/main/java/cn/oa/platform/web/controller/LegacyApiController.java` | v2 旧风格 API | 暴露 `/api/message/*`、`/api/schedule/*`、`/api/todo/*`、`/api/notice/*`、`/api/report/...`,**与 v2 命名约定 `/api/v1/msg/...` 冲突**;`MsgNotificationController` 已有 `/api/message/notifications` 新版;前端 `menuConfig.ts` 是否真调它需要查 | **审**:若前端 menuConfig 走它,先迁路由再删;否则删 |

---

## B. v2 收口期未提交的工作区产物 (**大概率应保留,作为下一笔 commit 内容**)

这些与 `docs/v2/business-closure-progress.md` 2026-06-07 的"已完成"清单一一对应,**不是 v1 残留**。

| # | 路径 | 数量 | 说明 |
|---|------|------|------|
| B1 | `oa-admin` supply 全套 (Controller/Entity/Mapper/Service) | 12 个 | 办公用品模块,收口阶段产物 |
| B2 | `oa-finance` contract + payment 全套 | 9 个 | 财务合同/付款,收口阶段产物 |
| B3 | `oa-hr-employee` extra (contract/change/certificate/education) | 9 个 | 员工档案扩展,收口阶段产物 |
| B4 | `oa-hr-performance` eval/goal | 4 个 | 绩效评估/目标,收口阶段产物 |
| B5 | `oa-hr-recruitment` candidate/interview/offer | 6 个 | 招聘,收口阶段产物 |
| B6 | `oa-hr-training` enroll/plan/session | 6 个 | 培训,收口阶段产物 |
| B7 | Flyway `V958` `V965` `V966` `V992` | 4 个 | 平台升级补丁,已在 v2-platform 其它分支 commit,本地还没 add |
| B8 | `oa-platform-web/.../LegacyApiController.java` | (重复见 A2) | 见 A2 |
| B9 | 前端 7 个新 view (`views/oa/{knowledge,task,performance,supply,recruitment,training,finance-contract,archive/extra}/index.vue`) | 8 个 | 收口阶段前端入口 |
| B10 | `code/frontend/src/api/businessModules.ts` | 1 个 | 业务模块 API 封装 |
| B11 | `code/frontend/playwright.config.ts` + `tests/` | 多文件 | 前端 E2E 脚手架,`pnpm test:e2e` 用 |
| B12 | `docs/v2/business-closure-progress.md` + `release-readiness.md` | 2 个 | v2 收口文档 |

**结论**: B 类**全部应保留**,与"删旧用新"无关。下一笔 commit 把它们一起 add。

---

## C. 工作区已修改但未提交 (M) — 逐文件确认,只动 v1 风格部分

63 个 ` M` 文件,**绝大多数是 v2 收口期的 controller 重构**。需要你逐项确认哪些是真"v1 风格"、哪些是 v2 风格微调。

高风险 6 个 (已删除的旧 Flyway 编号,需保留删除意图):
- ` D V972__mt_core_tables.sql` → 已迁到 `V965`,**保留删除**
- ` D V973__mt_permissions.sql` → 已迁到 `V966`,**保留删除**
- ` D V988__msg_notification_types.sql` → 已迁到 `V958`,**保留删除**

其余 60 个 ` M` 文件需逐个 `git diff` 确认是否回滚。

---

## D. 推荐执行方案 (等你说 "go")

**方案 1 — 最小动作 (推荐先做)**:
1. 物理删除 A1 (`MintToken.java`)
2. A2 (`LegacyApiController`) **保留**,后续单独 issue 处理
3. 工作区 `git add .` + `git commit -m "v2 收口阶段未提交产物入仓"` 把 B + C 中非 v1 部分一次入仓

**方案 2 — 激进清空**:
1. A1 + A2 一起删
2. `git add .` + `git commit` 把 B 入仓
3. ` M` 文件暂不处理 (留待各自 commit)

**方案 3 — 完全重置**:
1. `git reset --hard v2-platform` ← **违反微信护栏,绝不建议**

---

## E. 我**不会**做的事 (微信护栏 + CLAUDE.md 红线)

- 不会执行 `git reset --hard` / `rm -rf` / `git push`
- 不会修改 `CLAUDE.md` / `.mcp.json` / 任何 Flyway 脚本 (`V958/V965/V966/V992`)
- 不会动 `code/backend/oa-platform-{common,security,web}` 平台层结构
- 不会自动 commit/push 任何东西——所有写操作需要你显式 "go"
- 不会仅凭猜测删除 60 个 ` M` 文件——会先 `git diff` 给你看
