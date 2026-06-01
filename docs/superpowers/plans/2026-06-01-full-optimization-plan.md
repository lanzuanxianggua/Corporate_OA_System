# 企业OA全面优化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use Workflow 多 Agent 并行执行。每个审计 agent 独立扫描, 修复 agent 按优先级派发。步骤使用 checkbox (`- [ ]`) 语法。

**目标:** 对 OA 系统三端进行系统性全面优化, 覆盖安全/Bug/性能/代码质量/架构/前端体验 6 大维度

**架构:** 审计→修复→验证三阶段。审计阶段 7 个 agent 并行扫描; 修复阶段按 P0→P4 优先级派发修复 agent; 验证阶段编译检查 + 变更汇总

**Tech Stack:** Spring Boot 3.4.5, MyBatis-Plus 3.5.9, Redis, JWT, Vue 3 + Element Plus + TypeScript, uni-app

---

## Phase 1: 全域审计 (Workflow 并行)

### 审计任务 A1: 后端安全审计

**目标:** 扫描所有 Controller 的越权防护、SQL 注入、敏感信息泄露

- [ ] **A1-1: 越权检查扫描**
  - 读取 `oa-web/src/main/java/cn/oa/controller/` 下全部 35 个 Controller
  - 对每个 public 方法检查 `@RequireAdmin`/`@RequireRole` 注解覆盖
  - 特别关注: 查询他人数据的分页 page() 方法是否做数据权限隔离
  - 输出: 缺少注解的方法列表

- [ ] **A1-2: SQL 注入扫描**
  - 搜索 `oa-service/src/main/java/cn/oa/service/` 下所有 Java 文件
  - 搜索 MyBatis XML mapper 文件中 `${}` 的使用
  - 搜索原生 SQL (`@Select`、`Statement`)
  - 输出: 存在 `${}` 拼接的文件和行号

- [ ] **A1-3: 敏感信息泄露扫描**
  - 搜索返回实体中是否含 password/token/secret 字段
  - 搜索日志打印中是否含敏感数据
  - 搜索 `@JsonProperty(access = WRITE_ONLY)` 覆盖情况
  - 输出: 泄露风险点

- [ ] **A1-4: isAdmin() 实现扫描**
  - 搜索所有 `isAdmin` 和 `isAdminUser` 方法定义
  - 检查 `contains("ADMIN")` 子串匹配问题
  - 检查 Redis 和 DB 两种路径的不一致性
  - 输出: 重复方法位置清单

### 审计任务 A2: 后端 Bug 审计

**目标:** 扫描空指针、资源泄漏、并发问题、异常处理不当

- [ ] **A2-1: 空指针/并发风险扫描**
  - 搜索 `catch (Exception ignored)` / `catch (...) {}` 模式
  - 搜索 `static` 修饰的集合字段 (如 `sessions` map)
  - 搜索未判空的 `getter` 调用链
  - 输出: 高风险位置清单

- [ ] **A2-2: SQL N+1 扫描**
  - 搜索 for/while 循环中的 `selectById` / `getById` 调用
  - 输出: N+1 位置清单 (行号、涉及表)

- [ ] **A2-3: 事务边界扫描**
  - 搜索 Service 方法中涉及多表操作但缺少 `@Transactional` 的方法
  - 搜索 `@Transactional` 中调用远程/异步操作的模式
  - 输出: 事务边界问题

### 审计任务 A3: 后端代码质量与架构审计

**目标:** 扫描重复代码、长方法、大型类、类型不一致

- [ ] **A3-1: 重复代码检测**
  - 搜索 isAdmin 私有方法重复 (预计 8-9 处)
  - 搜索 `findRequired(request, "empId")` 类型参数提取重复
  - 搜索 `STATUS_TEXT` 数组重复定义
  - 搜索 export 方法结构重复
  - 输出: 重复代码位置清单

- [ ] **A3-2: 大型类/长方法检测**
  - 检查 WorkflowServiceImpl (1367 行) 的方法长度
  - 搜索 100 行以上的方法
  - 输出: 需要分解的类/方法清单

- [ ] **A3-3: 类型一致性检查**
  - 比较实体类 status 字段类型 (Integer vs String)
  - 检查 @JsonFormat 覆盖情况
  - 输出: 类型不一致清单

### 审计任务 A4: 前端审计

**目标:** 扫描 TypeScript 类型安全、代码重复、API 一致性

- [ ] **A4-1: JWT 解析审计**
  - 对比 `store/user.ts`、`utils/request.ts`、`router/index.ts` 中 JWT payload 解析实现
  - 检查 Unicode 处理差异
  - 输出: 差异明细

- [ ] **A4-2: Token 刷新机制审计**
  - 检查 `utils/request.ts` 中 `pendingRequests` 数组在刷新失败时的处理
  - 确认请求是否被孤立
  - 输出: Bug 确认和修复建议

- [ ] **A4-3: 类型定义审计**
  - 读取 `types/api.ts` 检查字段完整性
  - 搜索 `as any` / `@ts-ignore` 使用频率
  - 检查 PageResult 是否缺失分页辅助字段
  - 输出: 类型改进清单

### 审计任务 A5: 移动端审计

**目标:** 扫描 API 类型、平台兼容性

- [ ] **A5-1: API 类型审计**
  - 对比移动端 API 文件与前端 API 文件的类型签名差异
  - 输出: 需要添加类型的函数清单

- [ ] **A5-2: 上传和刷新机制审计**
  - 检查 upload() 函数对 401 的处理
  - 输出: 需要添加刷新逻辑的位置

---

## Phase 2: 修复执行 (按优先级)

### 任务 F1: P0 安全修复 — 提取公共 AuthUtil 并替换重复 isAdmin

**Files:**
- Create: `oa-common/src/main/java/cn/oa/common/utils/AuthUtil.java`
- Modify: `oa-web/src/main/java/cn/oa/controller/BusinessTripController.java`
- Modify: `oa-web/src/main/java/cn/oa/controller/EmployeeController.java`
- Modify: `oa-web/src/main/java/cn/oa/controller/ExpenseController.java`
- Modify: `oa-web/src/main/java/cn/oa/controller/LeaveApplyController.java`
- Modify: `oa-web/src/main/java/cn/oa/controller/LoanController.java`
- Modify: `oa-web/src/main/java/cn/oa/controller/OutingController.java`
- Modify: `oa-web/src/main/java/cn/oa/controller/OvertimeController.java`
- Modify: `oa-web/src/main/java/cn/oa/controller/PurchaseController.java`
- Modify: `oa-service/src/main/java/cn/oa/service/impl/WorkflowServiceImpl.java`

- [ ] **F1-1: 创建 AuthUtil.java**

```java
package cn.oa.common.utils;

import cn.oa.common.constant.RoleConstants;
import cn.oa.common.service.RedisService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@UtilityClass
public class AuthUtil {

    private static RedisService redisService;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 初始化 (由 Spring 容器在启动时调用)
     */
    public static void init(RedisService redisService) {
        AuthUtil.redisService = redisService;
    }

    /**
     * 判断员工是否为管理员 (精确匹配，非子串匹配)
     */
    public static boolean isAdmin(Long empId) {
        if (empId == null || redisService == null) {
            return false;
        }
        try {
            Object rolesObj = redisService.get("roles:" + empId);
            if (rolesObj == null) return false;
            // 尝试解析 JSON 数组
            String rolesStr = rolesObj.toString().trim();
            if (rolesStr.startsWith("[")) {
                List<String> roles = OBJECT_MAPPER.readValue(rolesStr, new TypeReference<List<String>>() {});
                return roles.contains(RoleConstants.ADMIN);
            }
            // 逗号分隔的字符串
            String[] parts = rolesStr.replace("[", "").replace("]", "").split(",");
            for (String part : parts) {
                if (part.trim().equals(RoleConstants.ADMIN)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取当前请求员工 ID 并强制只能访问自己的数据 (非管理员)
     */
    public static Long enforceOwnData(Long currentEmpId, Long targetEmpId) {
        if (targetEmpId == null || !targetEmpId.equals(currentEmpId)) {
            if (!isAdmin(currentEmpId)) {
                return currentEmpId;
            }
        }
        return targetEmpId;
    }
}
```

- [ ] **F1-2: 在 Spring 启动时初始化 AuthUtil**
  - 在 `oa-web` 的 Spring Boot 启动类或配置类中添加 `@PostConstruct` 初始化

```java
// 在 oa-web 的某个 @Configuration 类中添加:
@PostConstruct
public void initAuthUtil() {
    AuthUtil.init(redisService);
}
```

- [ ] **F1-3: 替换 BusinessTripController 中的 isAdmin 方法**
  - 删除私有 `isAdmin(Long empId)` 方法
  - 删除 `@Autowired RedisService redisService`
  - 将 page() 中的授权逻辑改为:
```java
Long currentEmpId = WebUtil.getEmpId(request);
if (empId == null || !empId.equals(currentEmpId)) {
    if (!AuthUtil.isAdmin(currentEmpId)) {
        empId = currentEmpId;
    }
}
```

- [ ] **F1-4: 替换 EmployeeController**
  - 删除私有 isAdmin 方法 + redisService 注入
  - 修改: `if (!currentEmpId.equals(empId) && !AuthUtil.isAdmin(currentEmpId))`

- [ ] **F1-5 至 F1-11: 替换其他 7 个 Controller**
  - 同样模式替换 ExpenseController, LeaveApplyController, LoanController, OutingController, OvertimeController, PurchaseController, WorkflowController

- [ ] **F1-12: 统一 WorkflowServiceImpl 的 admin 检查**
  - 将 `isAdminUser()` 改为使用 AuthUtil.isAdmin()
  - 删除旧方法

- [ ] **F1-13: 编译验证**
```bash
cd E:/JavaProject/Corporate_OA_System/code/backend && mvn compile -pl oa-web -am -q
```

### 任务 F2: P0 安全修复 — JWT 安全加固

**Files:**
- Modify: `oa-common/src/main/java/cn/oa/common/utils/JwtUtil.java`
- Modify: `oa-common/src/main/java/cn/oa/service/impl/AuthServiceImpl.java`
- Modify: `oa-common/src/main/java/cn/oa/vo/LoginVO.java`

- [ ] **F2-1: 修改 LoginVO 添加 refreshToken 独立字段**
  - 保留 `accessToken` (2h), 添加 `refreshToken` (7d)

- [ ] **F2-2: 修改 JwtUtil 添加两种 token 生成**
  - `generateAccessToken(empId, empName)` → 2h 过期
  - `generateRefreshToken(empId, empName)` → 7d 过期
  - 在 JWT claims 中添加 `tokenType` 区分

- [ ] **F2-3: 实现 refresh 端点**
  - `AuthService.refreshToken(String refreshToken)` 验证 refresh token, 签发新 access token

- [ ] **F2-4: 编译验证**

### 任务 F3: P0 安全修复 — 权限模型加固

**Files:**
- Modify: `oa-common/src/main/java/cn/oa/common/interceptor/AuthInterceptor.java`

- [ ] **F3-1: 修复 @RequirePermission 多角色循环**

```java
// 修复前: for 循环在第一个角色后 break, 仅检查第一个角色
// 修复后: 遍历所有角色, 任一角色有权限即可
boolean hasPermission = false;
for (String role : roles) {
    Set<String> perms = ROLE_PERMISSIONS.get(role);
    if (perms != null && perms.contains(permission)) {
        hasPermission = true;
        break;
    }
}
if (!hasPermission) {
    throw new AuthException("无操作权限");
}
```

- [ ] **F3-2: 修复 LoginVO permissions 值**
  - 改为根据实际角色返回权限列表, 而非 `["*:*:*"]`
  - 非管理员权限从 `ROLE_PERMISSIONS` 映射获取

### 任务 F4: P1 Bug 修复 — N+1 查询

**Files:**
- Modify: `oa-service/src/main/java/cn/oa/service/impl/WorkflowServiceImpl.java`

- [ ] **F4-1: 修复 myPendingTasks N+1**

```java
// 修复前: 对每个 task 执行 processInstanceMapper.selectById()
// 修复后: 批量查询
List<Long> instanceIds = taskList.stream()
    .map(WfTask::getProcessInstanceId)
    .filter(Objects::nonNull)
    .distinct()
    .collect(Collectors.toList());
Map<Long, WfProcessInstance> instanceMap;
if (!instanceIds.isEmpty()) {
    List<WfProcessInstance> instances = processInstanceMapper.selectBatchIds(instanceIds);
    instanceMap = instances.stream().collect(Collectors.toMap(WfProcessInstance::getId, Function.identity()));
} else {
    instanceMap = Collections.emptyMap();
}
```

- [ ] **F4-2: 修复 getApprovalHistory N+1**
  - 同样批量 select 替代循环 selectById

- [ ] **F4-3: 编译验证**

### 任务 F5: P1 Bug 修复 — WebSocket 和条件引擎

**Files:**
- Modify: `oa-web/src/main/java/cn/oa/websocket/NotificationEndpoint.java`
- Modify: `oa-service/src/main/java/cn/oa/service/impl/WorkflowServiceImpl.java`

- [ ] **F5-1: NotificationEndpoint 添加 handleTransportError**

```java
@Override
public void handleTransportError(WebSocketSession session, Throwable exception) {
    Long empId = getEmpIdFromSession(session);
    if (empId != null) {
        sessions.remove(empId, session);
    }
    log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
}

private Long getEmpIdFromSession(WebSocketSession session) {
    // 从 session attributes 获取 empId
    Object empIdObj = session.getAttributes().get("empId");
    return empIdObj instanceof Long ? (Long) empIdObj : null;
}
```

- [ ] **F5-2: WorkflowServiceImpl 条件引擎修复**
  - `numCheck()` 使用 `BigDecimal.compareTo()` 替代 `==` 比较

```java
private boolean numCheck(double value, double target, String operator) {
    BigDecimal v = BigDecimal.valueOf(value);
    BigDecimal t = BigDecimal.valueOf(target);
    int cmp = v.compareTo(t);
    switch (operator) {
        case ">" : return cmp > 0;
        case ">=": return cmp >= 0;
        case "<" : return cmp < 0;
        case "<=": return cmp <= 0;
        case "==": return cmp == 0;
        case "!=": return cmp != 0;
        default  : throw new BusinessException("不支持的比较运算符: " + operator);
    }
}
```

- [ ] **F5-3: resolveDeptManager 添加日志**

```java
catch (NumberFormatException e) {
    log.warn("Failed to parse dept manager config: {}", conditionValue, e);
}
```

### 任务 F6: P2 代码质量 — 提取公共授权和数据访问方法

**Files:**
- Modify: `oa-common/src/main/java/cn/oa/common/utils/WebUtil.java`

- [ ] **F6-1: WebUtil 添加 enforceOwnDataAccess 方法**

```java
/**
 * 强制数据权限: 非管理员只能访问自己的数据
 * @return 实际使用的 empId (非管理员被强制为当前用户)
 */
public static Long enforceOwnDataAccess(Long currentEmpId, Long targetEmpId) {
    if (targetEmpId == null || !targetEmpId.equals(currentEmpId)) {
        if (!AuthUtil.isAdmin(currentEmpId)) {
            return currentEmpId;
        }
    }
    return targetEmpId;
}
```

- [ ] **F6-2: WebUtil 添加 buildEmployeeNameMap 方法**

```java
/**
 * 批量构建员工 ID → 姓名的映射
 */
public static Map<Long, String> buildEmployeeNameMap(SysEmployeeMapper mapper, Set<Long> empIds) {
    if (empIds == null || empIds.isEmpty()) return Collections.emptyMap();
    List<SysEmployee> employees = mapper.selectBatchIds(empIds);
    return employees.stream()
        .filter(e -> e.getEmpName() != null)
        .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName));
}
```

- [ ] **F6-3: 替换 8 个 Controller 中的重复导出方法**
  - 修改 BusinessTripController, ExpenseController, LeaveApplyController 等

### 任务 F7: P2 代码质量 — 统一状态码和常量

**Files:**
- Modify: 所有 Controller (去除 STATUS_TEXT 数组)
- Modify: `oa-common/src/main/java/cn/oa/common/constant/`

- [ ] **F7-1: 创建 BusinessStatus 常量类**

```java
package cn.oa.common.constant;

public class BusinessStatus {
    public static final int PENDING = 0;
    public static final int APPROVED = 1;
    public static final int REJECTED = 2;
    public static final int WITHDRAWN = 3;
    // 特殊状态
    public static final int CANCELED = 4;
    public static final int RETURNED = 5;

    public static final String[] LABELS = {"待审批", "已通过", "已驳回", "已撤回", "已取消", "已退回"};
    public static final String[] LABELS_LEAVE = {"待审批", "已通过", "已拒绝", "已撤回", "已取消"};

    public static String getLabel(int status, boolean isLeave) {
        String[] labels = isLeave ? LABELS_LEAVE : LABELS;
        if (status >= 0 && status < labels.length) return labels[status];
        return "未知";
    }
}
```

- [ ] **F7-2: 替换 Controller 中的 STATUS_TEXT 数组引用**

- [ ] **F7-3: 编译验证**

### 任务 F8: P3 前端修复 — JWT 解析公共函数

**Files:**
- Create: `frontend/src/utils/jwt.ts`
- Modify: `frontend/src/store/user.ts`
- Modify: `frontend/src/utils/request.ts`
- Modify: `frontend/src/router/index.ts`

- [ ] **F8-1: 创建 jwt.ts**

```typescript
export function parseJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const payload = token.split(".")[1];
    const decoded = decodeURIComponent(
      Array.from(atob(payload), (c) =>
        "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2)
      ).join("")
    );
    return JSON.parse(decoded);
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = parseJwtPayload(token);
  if (!payload || !payload.exp) return true;
  return Date.now() >= (payload.exp as number) * 1000;
}
```

- [ ] **F8-2: 替换 store/user.ts 中的内联解析**
  - 导入并使用 `parseJwtPayload(token)`

- [ ] **F8-3: 替换 utils/request.ts 中的内联解析**
  - 导入并使用 `parseJwtPayload(token)`

- [ ] **F8-4: 替换 router/index.ts 中的内联解析**
  - 导入并使用 `parseJwtPayload(token)` 和 `isTokenExpired(token)`

### 任务 F9: P3 前端修复 — Token 刷新孤立请求

**Files:**
- Modify: `frontend/src/utils/request.ts`

- [ ] **F9-1: 修复刷新失败时的孤立请求**

```typescript
// 刷新失败时, 拒绝所有 pending 请求
if (refreshFailed) {
  hideLoading();
  clearAuthAndRedirect();
  pendingRequests.forEach(({ reject }) => reject(new Error("登录已过期")));
  pendingRequests = [];
  isRefreshing = false;
  return Promise.reject(new Error("Token refresh failed"));
}
```

### 任务 F10: P3 移动端修复 — API 类型安全

**Files:**
- Create: `mobile/src/types/api.ts`
- Modify: `mobile/src/api/*.ts`

- [ ] **F10-1: 创建移动端类型定义 (与前端的 types/api.ts 保持一致的子集)**

- [ ] **F10-2: 为 API 函数添加泛型返回类型**

- [ ] **F10-3: 修复 upload() 函数添加 token 刷新**

### 任务 F11: P4 实体类型一致性

**Files:**
- Modify: `oa-model/src/main/java/cn/oa/entity/WfProcessInstance.java`
- Modify: `oa-model/src/main/java/cn/oa/entity/WfTask.java`
- Modify: `oa-model/src/main/java/cn/oa/entity/WfProcessDefinition.java`

- [ ] **F11-1: 统一 WfTask.status 为 Integer**

```java
// 修改前: private String status = "0";
// 修改后: private Integer status = 0;
```

- [ ] **F11-2: 统一 WfProcessInstance 缺失的 @JsonFormat**

- [ ] **F11-3: 编译验证**

---

## Phase 3: 验证

### 任务 V1: 编译构建验证

- [ ] **V1-1: 后端 Maven 编译**
```bash
cd E:/JavaProject/Corporate_OA_System/code/backend && mvn compile -pl oa-web -am -q 2>&1
```

- [ ] **V1-2: 前端构建检查**
```bash
cd E:/JavaProject/Corporate_OA_System/code/frontend && pnpm build 2>&1 | tail -20
```

### 任务 V2: 审计报告汇总

- [ ] **V2-1: 汇总所有审计发现 (每个 agent 输出的合并)**
- [ ] **V2-2: 汇总所有修复 commit 和变更范围**
- [ ] **V2-3: 列出未修复事项及原因**

---

## 执行顺序汇总

```
Phase 1 (并行审计)
├── A1: 后端安全审计
├── A2: 后端 Bug 审计
├── A3: 后端代码质量/架构审计
├── A4: 前端审计
└── A5: 移动端审计

Phase 2 (按优先级执行)
├── F1: P0 AuthUtil + 重复 isAdmin 替换
├── F2: P0 JWT 安全加固
├── F3: P0 权限模型修复
├── F4: P1 N+1 查询批量修复
├── F5: P1 WebSocket + 条件引擎修复
├── F6: P2 公共方法提取 (enforceOwnData + employeeNameMap)
├── F7: P2 状态码常量统一
├── F8: P3 前端 JWT 解析统一
├── F9: P3 前端 Token 刷新修复
├── F10: P3 移动端 API 类型安全
└── F11: P4 实体类型一致性

Phase 3 (验证)
├── V1: 编译构建验证
└── V2: 变更汇总
```
