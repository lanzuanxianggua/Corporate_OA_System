# 前端布局体检报告

**日期**: 2026-06-08
**范围**: `code/frontend/src/`
**目的**: 排查"前端页面布局很乱"的根因
**改动**: **本报告不修改任何代码**,只给修复优先级与可执行方案

---

## 一、规模速览

| 维度 | 数据 |
|---|---|
| 业务模块 (后端 Maven) | 18 |
| Java 源文件 | 407 个 main + 52 个 test |
| 前端 .vue 文件 | 87 |
| 前端 .ts 文件 (src) | 62 |
| `src/router/index.ts` 行数 | **592** (单文件) |
| `src/layout/index.vue` 行数 | 219 |
| `src/layout/menuConfig.ts` 行数 | 172 |
| `src/components/` 公共组件数 | **2** (`ApprovalTimeline.vue`, `WorkflowDesigner.vue`) |
| 路由数 | ~70 |
| 业务 view 目录数 (含深嵌套) | **37** 个 `views/oa/...` 子树 |
| scoped style 覆盖 | **6/87 = 6.9%** |
| el-table 使用 | 76 处 |
| v-loading 使用 | 51 处 |
| el-pagination 使用 | 34 处 |

---

## 二、布局"乱"的 7 个真因

### 乱因 1 — Router 单文件 592 行,菜单/路由信息重复 2 份

`router/index.ts` 与 `layout/menuConfig.ts` **双源真相**:同一个 `path` 在两处各写一遍,新页面要改两处,极易漂移。

证据:
- `router/index.ts` 第 76 行定义 `oa/leave/apply`,`menuConfig.ts` 第 70 行又写一遍
- 收口阶段新增的 8 条业务闭环路由,在 `router` 末尾用 `// ── Missing business modules ──` 注释追加,在 `menuConfig.ts` 末尾用裸对象追加

### 乱因 2 — "请假" 业务有 3 套入口并存

| 路径 | 组件 | 状态 |
|---|---|---|
| `/oa/leave/apply` | `views/oa/leave/apply/index.vue` | v1 风格老入口,`views/oa/` 平铺 |
| `/oa/leave/approval` | `views/oa/leave/approval/index.vue` | v1 风格 |
| `/hr-leave/my-leaves` | `views/hr-leave/MyLeaves.vue` | v2 新入口,PascalCase 命名 |
| `/hr-leave/pending-approvals` | `views/hr-leave/PendingApprovals.vue` | v2 新入口 |
| `/hr/leave/index.vue` | `views/hr/leave/index.vue` | **孤儿文件** — router 不引用,无菜单 |

**根因**: v1→v2 收口期同业务先新建 `hr-leave/`,旧 `oa/leave/` 没拆。后端 `oa-hr-leave` 模块已经收口 (commit `efbc713`)、前端 hr-leave 也接好了 (commit `e8e1a8f`),但 `oa/leave/*` 这套历史入口**没下线**。

### 乱因 3 — 3 个"占位/孤儿" Vue 文件没人清理

| 文件 | 大小 | 状态 |
|---|---|---|
| `views/hr-leave/IndexView.vue` | 8 行 | 内容是 `<h2>请假管理 (待实现)</h2>`,真页面在 `MyLeaves.vue` / `PendingApprovals.vue` |
| `views/workflow/IndexView.vue` | 8 行 | `<h2>行政审批 (待实现)</h2>`,真页面在 `views/oa/workflow/*` |
| `views/error/NotFoundView.vue` | - | `views/error/404.vue` 已存在,这是重复 404 |
| `views/hr/leave/index.vue` | 434 行 | 整个 vue,但 router 不引用 |

### 乱因 4 — 公共组件库几乎为空 (2/87),业务页面大量重复自造组件

整个 `src/components/` 只有 `ApprovalTimeline.vue` + `WorkflowDesigner.vue`。
- `<el-card shadow="hover" ...>` 卡片样式 87 个 vue 写 87 遍
- `<el-row :gutter="20" class="mb-5">` 统计卡片 87 个 vue 写 87 遍
- `style="box-shadow:0 2px 12px rgba(0,0,0,.06)"` 内联硬编码阴影 8+ 处
- 顶部 "工具栏" 模式 (筛选条件 + 搜索按钮) 没有抽出 `SearchBar` / `PageToolbar`
- 表格 + 分页器 + loading 三角组合 76+ 处无封装

### 乱因 5 — 布局风格混用 (Tailwind 任意值 + 行内 style + 全局 class)

| 风格 | 示例 | 比例 |
|---|---|---|
| Tailwind 任意值 `text-[#409EFF]` `bg-[#f5f7fa]` | 72 个文件 | 83% |
| 行内 `style="box-shadow:..."` | dashboard / workbench / hr-leave 等多文件 | ~10% |
| 全局 class `.page` | **0 处使用** | 0% |
| `el-row`/`el-col` 24 栅格 + Tailwind flex 混用 | 几乎所有页面都有 | 混乱 |

**根因**: 早期用 `el-row`/`el-col` 24 栅格,后期改用 Tailwind flex/grid,迁移未完成。新写的页面用 Tailwind,老页面用 el-row,同一页面内部两种混用。

### 乱因 6 — scoped style 覆盖率 6.9%,样式泄漏风险

```
scoped style 使用: 6 个文件
总 vue 文件: 87
覆盖率: 6.9%
```

- `views/oa/dashboard/index.vue` `<style>` 段未加 scoped,echarts 容器 `height: 280px` 全局污染
- `views/oa/workbench/index.vue` 同样未加 scoped
- 大量 `views/oa/*` 表格页面 `<style>` 段**直接缺失**(grep `</style>` 在很多文件根本不存在,只有 `<template>` + `<script setup>`)

### 乱因 7 — 大文件,无内部分区/无组件拆分

| 文件 | 行数 | 内容类型 |
|---|---|---|
| `views/oa/approval-center/index.vue` | **604** | 8+ 业务 tab 全写一文件 |
| `views/hr/leave/index.vue` | **434** | 4 块业务全写一文件,**router 不引** |
| `views/oa/dashboard/index.vue` | 425 | 6 张 echarts + 4 个统计卡片全写一文件 |
| `views/oa/workbench/index.vue` | 407 | 工作台+打卡+待办+统计 4 区全写一文件 |
| `views/system/user/index.vue` | 375 | 增删改查全写一文件 |

> 项目里**完全没有**子目录形式的"按职责拆组件"——`/views/oa/leave/apply/index.vue` 里 form / table / dialog 全堆一起。

---

## 三、修复优先级 (由你勾选执行哪一档)

| 档 | 范围 | 估时 | 风险 |
|---|---|---|---|
| **P0 清理** (推荐先做) | 删 4 个孤儿文件 (3 个占位 + hr/leave/index.vue) + 合并 router/menuConfig 双源 → 单一 `routeMeta` 字典 | 2-3 h | 低,纯删/合并 |
| **P1 立约定** | 立 `src/components/{SearchBar.vue, PageWrapper.vue, StatCard.vue, TableBar.vue}` 4 件套;router 拆 `router/modules/*.ts` | 4-6 h | 中,需要批量改 70+ 路由 |
| **P2 抽组件** | 挑 dashboard / approval-center / 5 个 system/* 页面做"拆组件"示范,不强制全量 | 6-10 h | 中,样式调整 |
| **P3 统一风格** | 写 ESLint 规则:禁止行内 `style="..."`;强制 Tailwind;scoped 强制;大文件 >300 行警告 | 2 h + 持续治理 | 低 |

---

## 四、建议的**先动手**三步 (低风险,不破坏功能)

1. **删 4 个孤儿文件** — `views/hr-leave/IndexView.vue` `views/workflow/IndexView.vue` `views/error/NotFoundView.vue` `views/hr/leave/index.vue` (router 不引,纯死代码)
2. **合并 router/menuConfig 双源** — 新建 `src/router/meta.ts` 作为唯一 source of truth,`menuConfig.ts` 自动 derive
3. **建 4 个公共组件骨架** — `PageWrapper.vue` `SearchBar.vue` `StatCard.vue` `TableBar.vue`,先放骨架不动业务页,等后续整改

---

## 五、我不做的事 (等你显式指令)

- 不动 router 拆模块 (改动面大,需要先把 menuConfig 同步)
- 不动业务页面内部重构
- 不重命名目录
- 不删任何 `views/oa/leave/*` v1 风格入口 (虽然 P2 该下,但要先确认后端没人在用)
- 不跑 `pnpm build` 验证 — 你说"布局乱"是视觉描述,不是我跑构建能解决的
