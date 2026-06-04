# 08 - Corporate OA System v2 开发-测试-Review 闭环 SOP

> 版本: v2.0-draft
> 日期: 2026-06-04
> 状态: **Phase 1 设计中**
> 前置阅读: 全部其他 v2 文档

---

## 1. 总流程图

```
┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐
│ 需求/  │→ │ 设计   │→ │ 开发   │→ │ 自测   │→ │ 提 PR  │→ │ Review │
│ Issue  │  │ Spec   │  │ Code   │  │ Test   │  │ PR     │  │        │
└────────┘  └────────┘  └────────┘  └────────┘  └────────┘  └────────┘
     ↑                                                       │
     │                                                       ↓
┌────────┐                                            ┌────────┐
│ 文档/  │ ← 部署 Staging ← CI 通过 ← 合并 Main ← Review 通过
│ 复盘  │                                            └────────┘
└────────┘
```

---

## 2. 阶段 1 - 需求与设计

### 2.1 Issue 模板

**Feature Request**：

```markdown
## 需求
[一句话描述]

## 背景
[为什么需要这个功能]

## 用户故事
作为 [角色]
我希望 [功能]
以便于 [价值]

## 验收标准
- [ ] 标准 1
- [ ] 标准 2

## 设计
- 关联 spec: docs/v2/05-modules/XX-{module}.md
- UI 原型: [Figma 链接]

## 估算
- 设计: Xh
- 开发: Xh
- 测试: Xh

## 标签
feature, P1, hr-leave
```

**Bug Report**：

```markdown
## 描述
[发生了什么]

## 复现步骤
1. 步骤 1
2. 步骤 2

## 期望
[应该发生什么]

## 实际
[实际发生什么]

## 环境
- 版本: v2.0.0
- 浏览器: Chrome 120
- 复现率: 100%

## 截图
[图片]

## 标签
bug, P1, hr-leave
```

### 2.2 设计 spec

每个 Feature 必须有 spec（在 `docs/v2/05-modules/` 下，按 TEMPLATE 编写），spec 必须包含：
- §1 模块定位
- §2 目录结构
- §3 数据模型
- §4 业务规则
- §5 接口清单
- §6 错误码
- §7 权限与数据权限
- §8 工作流接入
- §9 前端调用
- §10 测试计划
- §11 实施任务
- §12 验收标准

**设计 review**：
- 模块 owner
- 架构师
- 测试 lead

### 2.3 设计 Checklist

- [ ] 命名一致（与 `01-architecture.md` 命名规范）
- [ ] 表结构与 `02-database.md` 一致
- [ ] 接口与 `03-api-spec.md` 一致
- [ ] 错误码在白名单内
- [ ] 权限码在白名单内
- [ ] 数据范围合理
- [ ] 工作流接入正确
- [ ] 前端页面规划
- [ ] 测试用例覆盖业务规则
- [ ] 验收标准可量化

---

## 3. 阶段 2 - 开发

### 3.1 分支命名

**格式**：
```
{type}/{issue-id}-{short-description}
```

**示例**：
- `feat/123-hr-leave-create`
- `fix/456-leave-overlap-check`
- `refactor/789-dept-tree`

### 3.2 开发步骤

按 `01-architecture.md` 分层：
1. **DDL**：写 Flyway 迁移脚本
2. **Entity**：写 PO（继承 BaseEntity）
3. **DTO/VO**：写出入参对象（加 Swagger 注解）
4. **Enums**：写业务枚举
5. **Mapper**：写 Mapper 接口（继承 BaseMapper）
6. **Mapper XML**：复杂 SQL（避免 Controller/Service 手写）
7. **Service 接口**：业务接口
8. **ServiceImpl**：业务实现
9. **Controller**：REST API（薄层）
10. **Callback**：工作流回调（如有）
11. **Tests**：单元/集成测试
12. **Frontend**：API 客户端 + 页面

### 3.3 开发规范

**后端**：
- ✅ Java 17 语法（var、record、sealed、switch 表达式）
- ✅ Lombok + MapStruct
- ✅ 不写 System.out.println（用 log）
- ✅ 不吞异常（catch 后必须处理或抛出）
- ✅ 事务边界在 Service
- ✅ Controller 禁用业务逻辑
- ✅ 慢查询 > 500ms 加索引或 @Cacheable

**前端**：
- ✅ Vue 3 `<script setup>` 语法
- ✅ TypeScript strict 模式
- ✅ 不直接修改 props
- ✅ 用 Pinia 而非 Vuex
- ✅ 用 VueUse 工具
- ✅ Element Plus 组件库

### 3.4 开发期间 Self-Review

完成每个类后自审：
- 命名清晰
- 没有重复代码
- 异常处理完整
- 注释说明"为什么"而不只是"做什么"
- 测试覆盖核心路径

---

## 4. 阶段 3 - 自测

### 4.1 本地测试清单

- [ ] 单元测试全过：`mvn test`
- [ ] 集成测试全过：`mvn verify`
- [ ] 编译无 warning：`mvn -DskipTests package`
- [ ] Spotless 格式：`mvn spotless:check`
- [ ] Checkstyle：`mvn checkstyle:check`
- [ ] 前端 lint：`pnpm lint`
- [ ] 前端 type check：`pnpm type-check`
- [ ] 前端单元测试：`pnpm test:unit`
- [ ] 覆盖率达标：Service > 80%

### 4.2 手动测试

**后端**（用 curl/Postman/Insomnia）：
- [ ] 创建接口
- [ ] 查询接口
- [ ] 更新接口
- [ ] 删除接口
- [ ] 业务动作（revoke/approve/reject）
- [ ] 异常路径（参数错误/无权限/数据不存在）
- [ ] 数据权限（5 级）
- [ ] 幂等
- [ ] 限流
- [ ] 接口签名

**前端**（用浏览器）：
- [ ] 登录/登出
- [ ] 列表查询/分页
- [ ] 详情查看
- [ ] 表单提交
- [ ] 业务操作按钮
- [ ] 权限按钮显隐
- [ ] 错误提示
- [ ] Loading 状态
- [ ] 空状态
- [ ] 移动端响应式

### 4.3 跨浏览器测试（仅前端）

- [ ] Chrome
- [ ] Edge
- [ ] Firefox
- [ ] Safari（如可用）

---

## 5. 阶段 4 - 提 PR

### 5.1 PR 模板

```markdown
## 关联 Issue
Closes #123

## 改动说明
[简述改动内容和原因]

## 设计文档
- spec: docs/v2/05-modules/XX-{module}.md

## 改动类型
- [ ] 新功能
- [ ] 缺陷修复
- [ ] 重构
- [ ] 性能优化
- [ ] 文档

## 测试
- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 手动测试通过
- [ ] 覆盖率达标

## Checklist
- [ ] 遵循代码规范
- [ ] 单元测试已加
- [ ] 文档已更新
- [ ] 数据库迁移脚本（如有）
- [ ] 前端页面（如有）
- [ ] OpenAPI 注解完整
- [ ] 错误码在白名单
- [ ] 权限码在白名单

## 截图
[UI 改动截图]

## 部署注意
[任何部署相关的注意事项]
```

### 5.2 PR 标题

格式：`<type>(<scope>): <subject>`

示例：
- `feat(hr-leave): 添加请假撤回接口`
- `fix(workflow): 修复审批人未找到异常`

---

## 6. 阶段 5 - Review

### 6.1 Reviewer 分配

**自动分配**（`.github/CODEOWNERS`）：

```
# 模块 owner
/docs/v2/05-modules/05-platform-common.md  @platform-team
/docs/v2/05-modules/10-hr-leave.md         @hr-team
/code/backend/oa-hr-leave/                  @hr-team
/code/frontend/src/views/hr-leave/          @hr-team
```

**手动分配**：
- 复杂改动：1 个模块 owner + 1 个架构师
- 简单改动：1 个模块 owner
- Bug 修复：原 author（如适用）+ 1 个 reviewer

### 6.2 Review Checklist

#### 6.2.1 设计 review
- [ ] 符合 spec（如有 spec）
- [ ] 不破坏向后兼容
- [ ] 不引入技术债
- [ ] 性能可接受

#### 6.2.2 代码 review（后端）
- [ ] 命名清晰
- [ ] 类职责单一
- [ ] 方法不过长（< 50 行）
- [ ] 无重复代码（DRY）
- [ ] 异常处理正确
- [ ] 事务边界合理
- [ ] 权限校验完整
- [ ] 数据范围正确
- [ ] 日志输出合理
- [ ] SQL 已 EXPLAIN
- [ ] 索引已加
- [ ] 缓存使用合理
- [ ] 接口签名正确
- [ ] OpenAPI 注解完整
- [ ] 错误码在白名单

#### 6.2.3 代码 review（前端）
- [ ] 组件职责单一
- [ ] props 不可变
- [ ] emit 命名清晰
- [ ] 类型完整
- [ ] 错误处理完整
- [ ] Loading 状态
- [ ] 空状态
- [ ] 权限控制
- [ ] 表单校验
- [ ] 国际化占位（v2 留位）
- [ ] 性能（虚拟滚动/防抖/分页）

#### 6.2.4 测试 review
- [ ] 单元测试覆盖核心
- [ ] 边界条件测试
- [ ] 异常路径测试
- [ ] 集成测试覆盖 API
- [ ] 覆盖率达标
- [ ] 测试可读
- [ ] 无 flaky 测试

#### 6.2.5 安全 review
- [ ] SQL 注入防护
- [ ] XSS 防护
- [ ] CSRF 防护
- [ ] 权限校验
- [ ] 敏感数据脱敏
- [ ] 日志不含敏感信息
- [ ] 接口签名
- [ ] 限流

### 6.3 Review SLA

| 优先级 | Review SLA |
|--------|-----------|
| P0 | 1 小时内 |
| P1 | 4 小时内 |
| P2 | 1 个工作日 |
| P3 | 3 个工作日 |

### 6.4 Review 评论规范

**有建设性**：
- ❌ "这个不好"
- ✅ "建议用 MapStruct 替代手写 getter/setter，可以减少 30% 样板代码"

**聚焦问题**：
- 评论代码问题，不评价人
- 区分 "must fix" / "should fix" / "nit"

**示例**：

```
🟥 must fix:
- L42 缺少事务注解 `@Transactional`，会导致部分写入
- L65 权限校验缺失，应加 `@RequirePermission("hr-leave:leave:create")`

🟨 should fix:
- L88 SQL 未加索引，建议加复合索引 `(emp_id, status, create_time)`
- L102 重复代码可提取到 `LeaveBalanceCalculator`

🟦 nit:
- L15 命名 `dto1` 不清晰，建议 `createDto`
```

### 6.5 Review 流程

1. **首次 Review**：1 个 reviewer 看完后评论
2. **作者修改**：push commit（不强制 force-push）
3. **二次 Review**：原 reviewer 看 diff
4. **Approve**：满足所有 must fix
5. **LGTM**：所有 should fix 已讨论
6. **Merge**：作者或 reviewer merge

### 6.6 自动化 Review

**CodeQL**：自动扫描安全漏洞
**Sonar**：自动扫描代码异味、覆盖率
**Dependabot**：自动升级依赖
**codecov**：自动覆盖率报告

---

## 7. 阶段 6 - 合并与部署

### 7.1 Merge 策略

- **Squash and Merge**（推荐）：把多个 commit 合并为 1 个
- **Merge Commit**：保留完整历史
- **Rebase and Merge**：线性历史

**约定**：
- Feature → develop：Squash
- develop → main：Merge Commit
- Hotfix → main：Merge Commit

### 7.2 Merge 后

- [ ] 删除源分支
- [ ] 检查 CI 主干流水线
- [ ] 镜像构建
- [ ] Staging 部署
- [ ] 烟测

---

## 8. 阶段 7 - 监控与反馈

### 8.1 上线后 24 小时

- [ ] 监控面板正常
- [ ] 错误率 < 0.1%
- [ ] P99 < 1s
- [ ] 用户反馈收集
- [ ] Crash/Bug 修复

### 8.2 上线后 1 周

- [ ] 数据复盘
- [ ] 性能 review
- [ ] 用户反馈总结
- [ ] 文档/FAQ 更新

### 8.3 故障响应

| 级别 | 响应时间 | 通知 |
|------|----------|------|
| P0 | 立即（24/7）| 全员 |
| P1 | 4 小时 | 模块 + 架构 |
| P2 | 1 工作日 | 模块 |
| P3 | 3 工作日 | 模块 |
| P4 | 7 工作日 | 个人 |

---

## 9. 角色与职责

| 角色 | 数量 | 职责 |
|------|------|------|
| **架构师** | 1-2 | 架构决策、Code Review、Spec 审核 |
| **Tech Lead（每模块）** | 1 | 模块设计、Code Review、指导 |
| **后端开发** | 5+ | 后端开发、单元测试、bug 修复 |
| **前端开发** | 3+ | 前端开发、组件库维护、UI 测试 |
| **测试** | 2+ | 测试设计、E2E、性能测试 |
| **DevOps** | 1+ | CI/CD、监控、部署 |
| **产品/PM** | 1-2 | 需求、优先级、验收 |

---

## 10. 关键指标（KPI）

### 10.1 质量指标

| 指标 | 目标 | 监控 |
|------|------|------|
| 单元测试覆盖率 | > 80% | JaCoCo |
| 集成测试通过率 | 100% | CI |
| Sonar 评级 | A | SonarQube |
| 安全漏洞 | 0 High | CodeQL |
| PR review 时间 | < SLA | GitHub |
| 缺陷泄漏率 | < 5% | Jira |
| 缺陷修复时间 | < SLA | Jira |
| 上线回滚次数 | < 5% | GitHub |

### 10.2 性能指标

| 指标 | 目标 |
|------|------|
| P50 响应时间 | < 200ms |
| P95 响应时间 | < 500ms |
| P99 响应时间 | < 1s |
| 错误率 | < 0.1% |
| 可用性 | > 99.9% |
| MTTR（平均修复时间）| < 4h |
| MTBF（平均故障间隔）| > 30d |

### 10.3 效率指标

| 指标 | 目标 |
|------|------|
| 部署频率 | 每天 1+ |
| Lead Time（需求到上线）| < 3d |
| MTTR（从报警到恢复）| < 30min |
| Change Failure Rate | < 5% |

---

## 11. 沟通与会议

| 会议 | 频率 | 参与 | 时长 |
|------|------|------|------|
| 每日站会 | 每日 | 开发 | 15min |
| 周会 | 每周 | 全员 | 1h |
| Sprint 计划 | 每 2 周 | 全员 | 2h |
| Sprint 回顾 | 每 2 周 | 全员 | 1h |
| Spec Review | 按需 | 架构师 + 模块 | 1h |
| Tech Talk | 每月 | 全员 | 1h |
| Incident 复盘 | 故障后 24h | 相关方 | 1h |

---

## 12. 文档管理

### 12.1 文档分类

| 类型 | 位置 | 维护人 |
|------|------|--------|
| 架构/设计 | `docs/v2/` | 架构师 |
| API 文档 | `/swagger-ui.html` | 后端 |
| 用户手册 | `docs/user-guide/` | 产品 |
| 部署文档 | `docs/operations/` | DevOps |
| 变更日志 | `CHANGELOG.md` | 全员 |
| FAQ | `docs/faq.md` | 产品 |

### 12.2 文档更新

- PR 改动代码：同步更新相关文档
- Spec 改动：必须 review
- 每周清理：删除过期文档

---

## 13. 总结

v2 闭环 SOP 核心：
1. **需求 → Spec → Code → Test → Review → Deploy → Monitor** 完整闭环
2. **每阶段都有 checklist**，避免遗漏
3. **Review SLA 严格**，保障响应速度
4. **关键指标量化**，可衡量可改进
5. **自动化 Review**（CodeQL/Sonar/Codecov）减少人力
6. **明确的角色与职责**

**v2 不做**：
- 4 人以下小团队（流程太重）
- 一次性项目（适合大版本/长期维护）
