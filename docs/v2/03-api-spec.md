# 03 - Corporate OA System v2 API 规范

> 版本: v2.0-draft
> 日期: 2026-06-04
> 状态: **Phase 1 设计中**
> 前置阅读: `00-index.md`、`01-architecture.md`、`02-database.md`

---

## 1. 设计原则

### 1.1 RESTful 严格
- **资源**: 复数名词（`/leaves` 而非 `/leave`）
- **方法**: HTTP 动词（GET 读/POST 创建/PUT 全量/PATCH 部分/DELETE 删除）
- **状态码**: 200/201/204/400/401/403/404/409/422/500
- **幂等性**: 写操作必须支持幂等（Header `Idempotency-Key`）
- **HATEOAS**: v2 不实现（v3 考虑）

### 1.2 版本
- **URL 路径版本**: `/api/v1/...`
- **不兼容升级**: 升 `/api/v2/...`
- **向下兼容**: 同一 v1 至少维护 6 个月
- **同时只支持 2 个版本**: v1 + v2，超期下线

### 1.3 响应格式
**所有接口统一返回 `R<T>`**（v1 的 `R` 设计保留并强化）：
```json
{
  "code": 0,
  "message": "ok",
  "data": { ... },
  "traceId": "abc123def456",
  "timestamp": 1717420800000
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `code` | Integer | 是 | 0=成功，其他见错误码表 |
| `message` | String | 是 | 人类可读错误信息 |
| `data` | T | 否 | 业务数据，无数据时为 null |
| `traceId` | String | 是 | MDC traceId，用于日志关联 |
| `timestamp` | Long | 是 | 服务端时间戳（毫秒） |

**分页响应**：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [...],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 10
  }
}
```

---

## 2. 路径规范

### 2.1 路径格式
```
{PREFIX}/v{MAJOR}/{MODULE}/{RESOURCE_PLURAL}[/{ID}][/{SUB_RESOURCE}][/{ACTION}]
```

| 段 | 含义 | 示例 |
|----|------|------|
| PREFIX | 固定 `/api` | `/api` |
| MAJOR | 主版本 | `v1` |
| MODULE | 业务模块（单数） | `hr-leave` `workflow` `admin` |
| RESOURCE_PLURAL | 资源（**复数**） | `leaves` `tasks` `users` |
| ID | 资源 ID | `123` |
| SUB_RESOURCE | 子资源 | `123/attachments` |
| ACTION | 业务动作（`/actions/{verb}`） | `123/actions/revoke` |

### 2.2 路径示例

| 操作 | 路径 | 方法 |
|------|------|------|
| 查询我的请假列表 | `/api/v1/hr-leave/leaves` | GET |
| 查询请假详情 | `/api/v1/hr-leave/leaves/123` | GET |
| 提交请假申请 | `/api/v1/hr-leave/leaves` | POST |
| 撤回请假 | `/api/v1/hr-leave/leaves/123/actions/revoke` | POST |
| 重新提交 | `/api/v1/hr-leave/leaves/123/actions/resubmit` | POST |
| 我的余额 | `/api/v1/hr-leave/balances/me` | GET |
| 余额列表（管理） | `/api/v1/hr-leave/balances` | GET |
| 调整余额 | `/api/v1/hr-leave/balances/123/adjustments` | POST |
| 规则列表 | `/api/v1/hr-leave/rules` | GET |
| 更新规则 | `/api/v1/hr-leave/rules/123` | PUT |
| 审批通过 | `/api/v1/workflow/tasks/456/actions/approve` | POST |
| 审批驳回 | `/api/v1/workflow/tasks/456/actions/reject` | POST |
| 审批转交 | `/api/v1/workflow/tasks/456/actions/transfer` | POST |

### 2.3 路径命名规则
- **模块**: 全小写，连字符分隔（`hr-leave` `hr-attendance`）
- **资源**: 全小写，下划线分隔（**但 URL 用连字符**）（`leave_balances` 在表名，`leave-balances` 在 URL）
- **动作**: 动词原形（`approve`/`reject`/`transfer`/`revoke`/`resubmit`/`close`）

---

## 3. HTTP 方法

| 方法 | 幂等 | 安全 | 用途 |
|------|------|------|------|
| GET | 是 | 是 | 读 |
| POST | 否 | 否 | 创建、动作、复杂查询 |
| PUT | 是 | 否 | 全量更新 |
| PATCH | 是 | 否 | 部分更新 |
| DELETE | 是 | 否 | 删除（v2 软删） |

**业务动作统一用 POST + `/actions/{verb}`**：
- 不使用 PATCH 做业务动作（业务动作有副作用，不是属性修改）
- 不在 URL 里硬编码动词（`/approve` 路径），统一 `/actions/approve`

---

## 4. 错误码

### 4.1 错误码编码规则

**纯数字分段编码**（与 `RCode` 枚举对齐）：

| 范围 | 模块 | 说明 |
|------|------|------|
| 0 | 通用 | 成功 |
| 1-99 | 通用 | 通用错误（参数/校验/404） |
| 10001-10999 | 认证 | JWT/Token/签名 |
| 20001-20999 | 权限 | RBAC/数据权限 |
| 30001-30999 | 平台 | 限流/幂等 |
| 99001-99999 | 系统 | 内部错误/数据库/第三方 |

**业务模块错误码**：各模块在 Service 层通过 `BizException(RCode, "消息")` 抛出时使用对应范围的数字。模块内错误码由各模块 spec 定义。

### 4.2 错误码清单（核心）

| 编码 | 名称 | 描述 | HTTP |
|------|------|------|------|
| `0` | SUCCESS | 成功 | 200 |
| `00001` | BAD_REQUEST | 参数错误 | 400 |
| `00002` | VALIDATION_FAILED | 参数校验失败 | 422 |
| `00100` | NOT_FOUND | 资源不存在 | 404 |
| `00101` | METHOD_NOT_ALLOWED | 方法不允许 | 405 |
| `00102` | UNSUPPORTED_MEDIA_TYPE | 媒体类型不支持 | 415 |
| `01001` | UNAUTHORIZED | 未登录 | 401 |
| `01002` | TOKEN_EXPIRED | Token 过期 | 401 |
| `01003` | INVALID_TOKEN | Token 无效 | 401 |
| `01004` | SIGN_INVALID | 签名错误 | 401 |
| `02001` | FORBIDDEN | 无权限 | 403 |
| `02002` | DATA_PERMISSION_DENIED | 数据权限不足 | 403 |
| `03001` | RATE_LIMIT_EXCEEDED | 限流 | 429 |
| `03002` | IDEMPOTENT_CONFLICT | 幂等冲突 | 409 |
| `99001` | INTERNAL_ERROR | 服务内部错误 | 500 |
| `99002` | SERVICE_UNAVAILABLE | 服务暂不可用 | 503 |
| `99003` | DB_ERROR | 数据库错误 | 500 |
| `99004` | THIRD_PARTY_ERROR | 第三方服务错误 | 502 |
| `99999` | UNKNOWN | 未知错误 | 500 |

### 4.3 业务错误示例

各业务模块在 Service 层通过 `throw new BizException(RCode.X, "消息")` 抛出。错误消息为中文可读文本，错误码在 RCode 枚举中定义。示例：

| 模块 | 场景 | 抛出方式 |
|------|------|----------|
| HR 请假 | 假期类型无效 | `throw new BizException(RCode.PARAM_ERROR, "假期类型无效")` |
| HR 请假 | 余额不足 | `throw new BizException(RCode.BIZ_ERROR, "余额不足")` |
| HR 请假 | 日期重叠 | `throw new BizException(RCode.BIZ_ERROR, "请假日期重叠")` |
| 工作流 | 找不到审批人 | `throw new BizException(RCode.BIZ_ERROR, "找不到审批人")` |
| 工作流 | 任务已处理 | `throw new BizException(RCode.BIZ_ERROR, "任务已处理")` |
| 财务 | 预算不足 | `throw new BizException(RCode.BIZ_ERROR, "预算不足")` |

### 4.4 错误响应格式
```json
{
  "code": 10402,
  "message": "假期类型无效",
  "data": null,
  "traceId": "abc123def456",
  "timestamp": 1717420800000
}
```

**批量错误**（参数校验失败）：
```json
{
  "code": 10002,
  "message": "参数校验失败",
  "data": {
    "errors": [
      { "field": "startTime", "message": "开始时间不能为空" },
      { "field": "endTime", "message": "结束时间必须晚于开始时间" }
    ]
  },
  "traceId": "...",
  "timestamp": 1717420800000
}
```

---

## 5. 权限码

### 5.1 编码规则
**3 段式**：
```
{MODULE}:{RESOURCE}:{ACTION}
```

- `MODULE`: 模块（`hr-leave`, `hr-attendance`, `workflow`）
- `RESOURCE`: 资源（**单数或带连字符**，如 `leave` `leave-balance` `task`）
- `ACTION`: 动作（来自白名单）

### 5.2 动作白名单
| 动作 | 含义 |
|------|------|
| `view` | 详情 |
| `list` | 列表 |
| `create` | 创建 |
| `update` | 更新 |
| `delete` | 删除 |
| `export` | 导出 |
| `import` | 导入 |
| `approve` | 审批通过 |
| `reject` | 审批驳回 |
| `revoke` | 撤回 |
| `resubmit` | 重新提交 |
| `transfer` | 转交 |
| `delegate` | 委托 |
| `init` | 初始化 |
| `adjust` | 调整 |
| `print` | 打印 |
| `archive` | 归档 |
| `close` | 关闭 |
| `publish` | 发布 |
| `suspend` | 暂停 |
| `enable` | 启用 |
| `disable` | 停用 |

白名单优先使用。超出白名单的动作码需在设计 review 中审批后方可使用。

### 5.3 权限码示例

```
hr-leave:leave:create           # 创建请假
hr-leave:leave:list             # 请假列表
hr-leave:leave:view             # 请假详情
hr-leave:leave:revoke           # 撤回请假
hr-leave:leave:resubmit         # 重新提交
hr-leave:leave-balance:view     # 我的余额
hr-leave:leave-balance:list     # 余额管理列表
hr-leave:leave-balance:init     # 初始化余额
hr-leave:leave-balance:adjust   # 调整余额
hr-leave:leave-rule:list        # 规则列表
hr-leave:leave-rule:update      # 更新规则
workflow:task:list              # 我的待办
workflow:task:view              # 任务详情
workflow:task:approve           # 审批通过
workflow:task:reject            # 审批驳回
workflow:task:transfer          # 转交
workflow:instance:view          # 流程实例详情
workflow:instance:revoke        # 撤回流程
```

### 5.4 权限与角色映射
**RBAC 角色 → 权限码**：
```
ADMIN    → 所有权限
HR       → hr-*:*，workflow:task:list/view
FINANCE  → fn-*:*，workflow:task:list/view/approve/reject/transfer
MANAGER  → workflow:task:list/view/approve/reject/transfer，*:*:list/view（仅本部门）
EMPLOYEE → *:leave:create/list/view（仅本人），*:*:view（仅本人）
```

**5 级数据权限**（与角色正交）：
```
SELF      → 员工本人数据
DEPT      → 本部门
DEPT_DOWN → 本部门及下级
COMPANY   → 全公司
ALL       → 全部（含跨公司，v2 不实现）
```

---

## 6. 请求与响应规范

### 6.1 请求头

| Header | 必填 | 描述 |
|--------|------|------|
| `Authorization` | 是 | `Bearer {accessToken}` |
| `Content-Type` | 是 | `application/json; charset=utf-8` |
| `Accept-Language` | 否 | `zh-CN` / `en-US`（v2 留位） |
| `X-Trace-Id` | 否 | 客户端生成的 traceId |
| `Idempotency-Key` | 否 | 写操作幂等键（24h 内同 key 不重复执行） |
| `X-Sign` | 否 | 写操作签名（HMAC-SHA256） |
| `X-Timestamp` | 配合 X-Sign | 时间戳（与服务器时间差 > 5min 拒绝） |
| `X-Request-Id` | 否 | 请求 ID |

### 6.2 响应头

| Header | 描述 |
|--------|------|
| `X-Trace-Id` | 服务端 traceId（与 body 中 traceId 一致） |
| `X-Rate-Limit-Remaining` | 当前窗口剩余请求数 |
| `X-Rate-Limit-Reset` | 窗口重置时间（秒） |
| `Content-Type` | `application/json; charset=utf-8` |

### 6.3 业务操作响应
**创建操作**（POST 201）：
```json
{
  "code": 0,
  "message": "ok",
  "data": { "id": 123, "applyNo": "HR20260604..." },
  "traceId": "...",
  "timestamp": 1717420800000
}
```

**动作操作**（POST /actions/{verb} 200）：
```json
{
  "code": 0,
  "message": "撤回成功",
  "data": null,
  "traceId": "...",
  "timestamp": 1717420800000
}
```

**删除操作**（DELETE 204）：无 body

---

## 7. 工作流 API（专项）

### 7.1 流程定义

| 操作 | 路径 | 方法 | 权限 |
|------|------|------|------|
| 创建定义 | `/api/v1/workflow/definitions` | POST | `workflow:definition:create` |
| 列表 | `/api/v1/workflow/definitions` | GET | `workflow:definition:list` |
| 详情 | `/api/v1/workflow/definitions/{id}` | GET | `workflow:definition:view` |
| 更新 | `/api/v1/workflow/definitions/{id}` | PUT | `workflow:definition:update` |
| 发布 | `/api/v1/workflow/definitions/{id}/actions/publish` | POST | `workflow:definition:update` |
| 废弃 | `/api/v1/workflow/definitions/{id}/actions/deprecate` | POST | `workflow:definition:update` |

### 7.2 流程实例

| 操作 | 路径 | 方法 | 权限 |
|------|------|------|------|
| 启动 | `/api/v1/workflow/instances` | POST | （由业务模块启动） |
| 详情 | `/api/v1/workflow/instances/{id}` | GET | `workflow:instance:view` |
| 我的发起 | `/api/v1/workflow/instances?initiator=me` | GET | `workflow:instance:list` |
| 撤回 | `/api/v1/workflow/instances/{id}/actions/revoke` | POST | `workflow:instance:revoke` |

### 7.3 审批任务

| 操作 | 路径 | 方法 | 权限 |
|------|------|------|------|
| 我的待办 | `/api/v1/workflow/tasks?assignee=me&status=PENDING` | GET | `workflow:task:list` |
| 已办 | `/api/v1/workflow/tasks?assignee=me&status=HANDLED` | GET | `workflow:task:list` |
| 详情 | `/api/v1/workflow/tasks/{id}` | GET | `workflow:task:view` |
| 审批通过 | `/api/v1/workflow/tasks/{id}/actions/approve` | POST | `workflow:task:approve` |
| 审批驳回 | `/api/v1/workflow/tasks/{id}/actions/reject` | POST | `workflow:task:reject` |
| 转交 | `/api/v1/workflow/tasks/{id}/actions/transfer` | POST | `workflow:task:transfer` |
| 委托 | `/api/v1/workflow/tasks/{id}/actions/delegate` | POST | `workflow:task:delegate` |
| 加签 | `/api/v1/workflow/tasks/{id}/actions/countersign` | POST | `workflow:task:approve` |
| 抄送 | `/api/v1/workflow/tasks/{id}/actions/cc` | POST | `workflow:task:view` |

### 7.4 回调接口

业务模块实现 `WfCallback` 接口，工作流引擎在状态变更时调用：

```java
public interface WfCallback {
    /** 流程启动后 */
    void onInstanceStarted(WfCallbackContext ctx);
    /** 任务被分配时 */
    void onTaskAssigned(WfCallbackContext ctx);
    /** 任务被处理时 */
    void onTaskHandled(WfCallbackContext ctx);
    /** 流程结束时 */
    void onInstanceEnded(WfCallbackContext ctx);
}
```

业务模块注册：
```java
@Component
public class HrLeaveWfCallback implements WfCallback {
    @Override
    public void onInstanceEnded(WfCallbackContext ctx) {
        if (ctx.getBusinessType().equals("HR_LEAVE")) {
            HrLeave leave = hrLeaveService.getById(ctx.getBusinessId());
            if (ctx.getEndStatus() == WfEndStatus.PASSED) {
                leave.setStatus(LeaveStatus.PASSED);
                leave.setApprovedTime(ctx.getEndTime());
                hrLeaveService.updateById(leave);
            } else if (ctx.getEndStatus() == WfEndStatus.REJECTED) {
                leave.setStatus(LeaveStatus.REJECTED);
                leave.setRejectedTime(ctx.getEndTime());
                leave.setRejectReason(ctx.getComment());
                hrLeaveService.updateById(leave);
            }
        }
    }
}
```

---

## 8. 安全

### 8.1 JWT

**Access Token**：
- 算法：HS256
- 过期：2 小时
- Payload: `{ sub: empId, iat, exp, roles: [...], perms: [...], dataPerm: "DEPT" }`
- Header: `Authorization: Bearer {token}`

**Refresh Token**：
- 存储：HttpOnly Cookie
- 过期：14 天
- 用途：换 Access Token

**Token 刷新**：
```
POST /api/v1/platform/auth/refresh
Body: { refreshToken: "..." }
Response: { accessToken: "...", refreshToken: "..." }
```

### 8.2 接口签名（写操作）

**规则**：
- 写操作（POST/PUT/PATCH/DELETE）必须带 `X-Sign` 和 `X-Timestamp`
- 算法：HMAC-SHA256
- 签名串：`{method}\n{path}\n{queryString}\n{sortedJsonBody}\n{timestamp}`
- 密钥：客户端 secret（与 empId 绑定）
- 时间戳与服务器时间差 > 5 分钟拒绝

**示例**：
```
POST /api/v1/hr-leave/leaves
X-Timestamp: 1717420800
X-Sign: a1b2c3d4...

签名计算（Node.js）：
const crypto = require('crypto');
const payload = [
  'POST',
  '/api/v1/hr-leave/leaves',
  '',
  JSON.stringify(sortedJson(body)),
  '1717420800'
].join('\n');
const sign = crypto.createHmac('sha256', secret).update(payload).digest('hex');
```

### 8.3 限流

**5 级限流**：
- IP 限流：100 req/min
- 用户限流：200 req/min
- 接口限流：依业务（详情见各模块）
- 写操作限流：50 req/min/用户
- 登录限流：5 次/15min（防爆破）

**实现**：Redis 令牌桶，注解 `@RateLimit(key="...", capacity=100, refill=10)`

### 8.4 幂等

**规则**：
- 写操作（POST 创建/PUT/DELETE）必须支持 `Idempotency-Key`
- 服务端存储 key → result 映射（Redis，TTL 24h）
- 同 key 重复请求：返回首次结果（不重执行）
- 不同 body 同 key：返回 409 IDEMPOTENT_CONFLICT

**实现**：注解 `@Idempotent(key = "#header.Idempotency-Key", ttl = 86400)`

### 8.5 数据权限

**5 级**（`@DataPermission(value = DataScope.DEPT)` 注解）：

| 值 | 含义 | 注入 SQL |
|----|------|----------|
| `SELF` | 仅本人 | `AND create_by = {empId}` |
| `DEPT` | 本部门 | `AND dept_id = {deptId}` |
| `DEPT_DOWN` | 本部门及下级 | `AND dept_id IN (SELECT id FROM sys_departments WHERE path LIKE '{deptPath}%')` |
| `COMPANY` | 全公司 | 无条件 |
| `ALL` | 全部 | 无条件 |

**使用**：
```java
@GetMapping("/leaves")
@RequirePermission("hr-leave:leave:list")
@DataPermission(value = DataScope.DEPT)  // 经理看本部门
public R<PageResult<HrLeaveVO>> list(HrLeaveQueryDTO query) {
    return R.ok(hrLeaveService.listWithDataPermission(query));
}
```

**Service 实现**：
```java
public PageResult<HrLeave> listWithDataPermission(HrLeaveQueryDTO query) {
    // MyBatis-Plus 自动注入 dept_id IN (...) 条件
    // 来自 DataPermissionContextHolder（ThreadLocal）
    return hrLeaveMapper.selectPage(query.toPage(), DataPermissionContextHolder.getWrapper());
}
```

---

## 9. 跨域（CORS）

**生产**：
- `Access-Control-Allow-Origin: https://oa.example.com`（具体域名，不用 `*`）
- `Access-Control-Allow-Credentials: true`（允许带 Cookie）
- `Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,OPTIONS`
- `Access-Control-Allow-Headers: Authorization, Content-Type, X-Trace-Id, X-Sign, X-Timestamp, Idempotency-Key`
- `Access-Control-Expose-Headers: X-Trace-Id, X-Rate-Limit-Remaining, X-Rate-Limit-Reset`
- `Access-Control-Max-Age: 86400`

**开发**：
- `*`（任意 origin，禁用 credentials）

---

## 10. 接口文档（OpenAPI 3.0）

- **生成工具**: springdoc-openapi-starter-webmvc-ui 2.x
- **UI 路径**: `/swagger-ui.html`
- **JSON 路径**: `/v3/api-docs`
- **规范**:
  - 所有 Controller 必须有 `@Tag(name = "...")` 标注
  - 所有方法必须有 `@Operation(summary = "...")`
  - 所有参数必须有 `@Parameter(description = "...")`
  - 所有响应必须有 `@ApiResponse(responseCode = "200", description = "...")`
  - DTO 必须有 `@Schema(description = "...")`

**模块分组**：`tag` 与模块名一致，便于按模块浏览。

---

## 11. 健康检查与监控

**Actuator 端点**：
- `/actuator/health` 综合健康
- `/actuator/info` 应用信息
- `/actuator/metrics` 指标
- `/actuator/prometheus` Prometheus 格式（生产暴露）

**自定义健康检查**：
- 数据库连接
- Redis 连接
- 磁盘空间
- 工作流引擎状态

---

## 12. 详细接口契约

完整接口契约见各模块详细设计：
- `05-modules/10-hr-leave.md` - HR 请假接口
- `05-modules/06-workflow.md` - 工作流接口
- `05-modules/07-admin.md` - 行政接口
- ...

每份模块 spec 必须包含：
1. 接口清单（表格）
2. 详细字段（每个 DTO/VO）
3. 业务规则（if-else 流程）
4. 状态机图
5. 错误码（子集）
6. 权限与数据权限
7. 前端调用示例
