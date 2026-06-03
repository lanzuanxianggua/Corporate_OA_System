# M0 重构准备清单

> 日期: 2026-06-02  
> 阶段: M0 - 重构准备  
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md`  
> Claude 工作流文档: `docs/superpowers/workflows/claude-code-oa-redesign-workflow.md`

---

## 1. M0 目标

M0 不迁移业务代码，目标是让后续 Claude Code / ultracode / 人工协作具备可靠上下文。

必须完成：

1. Claude 工作流不再引用其他项目。
2. 每个重构任务都有统一的任务模板和验收方式。
3. 明确当前仓库处于“旧聚合模块 + 新DDD模块并存”的过渡态。
4. 选定第一个试点闭环，并拆分为可执行任务。

---

## 2. 当前项目基线

| 维度 | 当前状态 |
|------|----------|
| 后端 | Java 17 + Spring Boot 3.4.5 + MyBatis-Plus + Maven 多模块 |
| 旧模块 | `oa-model`、`oa-mapper`、`oa-service`、`oa-web` 中仍保留大量旧实现 |
| 新模块 | 根 POM 已声明 `oa-platform`、`oa-workflow`、`oa-hr`、`oa-finance` 等新模块 |
| Web | `code/frontend` 独立 Vite 应用，已有 API 和 OA 页面 |
| Mobile | `code/mobile` uni-app，已有员工端页面和 API 子集 |
| 数据脚本 | `code/backend/sql/` 下存在多份全量、扩展、seed、修复脚本，需要收敛 |
| Claude 配置 | `.claude/workflows` 已修正为 Corporate OA System 语境 |

---

## 3. 已完成准备项

| 项目 | 状态 | 说明 |
|------|------|------|
| 重构文档补强 | 已完成 | 新增“十一、重构落地补强” |
| Claude workflow 文档 | 已完成 | 新增 `claude-code-oa-redesign-workflow.md` |
| workflow 项目上下文修正 | 已完成 | 清除 `ZhihuiDangjian`、`Gradle`、`Java 21`、`Sa-Token`、Linux 旧路径 |
| QA workflow 修正 | 已完成 | 改为 Maven backend + Web typecheck/build + mobile H5 build |
| CI 审计 workflow 修正 | 已完成 | 改为 MySQL/Redis/ES、Maven/Vite/uni-app 语境 |
| 第一个试点选择 | 已完成 | 选择 HR 请假审批闭环 |

---

## 4. 后续重构硬规则

1. 新功能只进新 DDD 模块，不再向旧 `oa-service` 增加业务能力。
2. 每次只迁移一个业务闭环，闭环必须包含后端、数据库、权限、前端/移动端、测试和文档。
3. 先写契约，再写实现。
4. 并行任务不得修改同一文件或同一 SQL 基线片段。
5. 每个任务完成后必须运行最小验收命令，无法运行要说明原因。

---

## 5. M0 门禁

| 检查项 | 命令/方式 | 通过标准 |
|--------|-----------|----------|
| 旧项目标识清理 | `rg "ZhihuiDangjian|Java 21|Gradle|Sa-Token|/home/rauio" .claude/workflows` | 无结果 |
| 文档存在 | 检查 `docs/superpowers/workflows/claude-code-oa-redesign-workflow.md` | 文件存在且可读 |
| 重构补强章存在 | 查找 `## 十一、重构落地补强` | 章节存在 |
| 试点任务拆分存在 | 检查 `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md` | 文件存在且包含任务波次 |

---

## 6. M1 进入条件

进入代码重构前，应先确认：

1. 当前 Git 工作区中哪些改动是用户已有改动，哪些是本轮文档/workflow 改动。
2. 是否创建独立分支，例如 `refactor/oa-redesign-m0` 或 `refactor/hr-leave-pilot`。
3. 是否确认第一个试点任务拆分。
4. 是否允许开始修改 `code/backend`、`code/frontend`、`code/mobile` 和 `code/backend/sql`。

---

## 7. 建议下一步

下一步进入 HR 请假审批闭环试点：

1. 先执行数据库与 API 契约任务。
2. 再实现 `oa-hr` 后端闭环。
3. 然后接入工作流回调、待办和消息。
4. 最后迁移 Web/移动端页面并补齐测试。
