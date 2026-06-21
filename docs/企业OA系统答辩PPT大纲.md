# 企业 OA 办公系统项目答辩 PPT（详细技术实现版）

## 第 1 页：封面
**标题**: 企业 OA 办公自动化系统  
**副标题**: 基于 Spring Boot 3.4 + Vue 3 的企业级办公管理平台  
**答辩人**: [姓名]  
**日期**: 2026 年 6 月

---

## 第 2 页：项目概述

### 系统定位
- 面向 200-5000 人企业的办公自动化系统
- 覆盖行政、人事、财务、审批、协同、资产、看板 8 大业务域
- 减少线下审批和人工统计成本，将日常办公流程线上化

### 核心目标
- **业务模块清晰**：50+ 功能模块，统一 CRUD 模式
- **审批流程可配置**：自研图结构工作流引擎，支持分级路由
- **权限可控**：RBAC 模型 + 注解式接口鉴权
- **数据可追溯**：逻辑删除 + 统一审计字段 + 完整的操作日志

### 技术概览（一句话总结）
```
前端： Vue 3 + TypeScript + Vite + Element Plus + Tailwind CSS + ECharts + Pinia
后端： Spring Boot 3.4 + Java 17 + MyBatis-Plus 3.5.9 + MySQL 8 + Redis + JWT
工具： Maven 多模块 + Flyway 迁移 + Docker Compose + GitHub Actions CI
```

---

## 第 3 页：技术架构（分层架构图 + 数据流详解）

### 完整数据流（一次登录请求的完整链路）
```
用户输入账号密码 → 前端 axios 请求 → Vite 代理 (/api → localhost:8080)
  → 过滤器链: CORS → RateLimitInterceptor(Redis限流)
    → AuthInterceptor(解析JWT Token,校验Redis存储)
      → Controller(@Validated参数校验)
        → Service(业务逻辑+@Transactional事务)
          → Mapper(MyBatis-Plus SQL)
            → MySQL/Redis → 响应 R<T> 返回 → 前端渲染
```

### 前端架构
```
src/
├── api/          # Axios 封装 + 各模块 API（auth.ts、leave.ts、attendance.ts...）
├── router/       # 路由配置 + 导航守卫（路由白名单：login、register）
├── store/        # Pinia 状态管理（user.ts 管理 Token 和权限）
├── views/        # 页面组件按模块分目录
├── components/   # 通用组件（EyeBall.vue 眼球动画、Pupil.vue 瞳孔动画）
├── utils/        # 工具（request.ts axios拦截器、chartTheme.ts ECharts主题）
└── types/        # TypeScript 类型定义（LoginDTO、LoginVO、ApiResponse...）
```

### 后端架构
```
oa-web (启动模块+Controller+Flyway)
  ├── oa-common   (R<T>响应、全局异常处理、JwtUtil、RedisService、拦截器)
  ├── oa-model    (Entity表映射、DTO入参、VO出参)
  ├── oa-mapper   (MyBatis-Plus Mapper接口 + XML)
  └── oa-service  (Service接口+Impl、@Transactional事务、Spring Event回调)
```

---

## 第 4 页：前端技术选型详解（结合代码）

### Vue 3 Composition API + TypeScript
**文件**: `code/frontend/src/views/login/index.vue`

使用 `<script setup lang="ts">` 方式组织逻辑，所有状态和方法直接定义在顶层：

```typescript
// 组件内无需 export default，直接在 <script setup> 中定义
const loading = ref(false);             // 响应式状态
const loginForm = reactive({...});      // 表单对象
const purplePos = computed(...);        // 计算属性，跟踪鼠标位置
// 无需 Options API 的 data/computed/methods 分离，逻辑内聚性更强
```

### Element Plus — 自动导入 + 暗黑适配
**文件**: `code/frontend/vite.config.ts`

```typescript
// unplugin-vue-components 自动注册 Element Plus 组件
// 页面中直接使用 <el-button>、<el-input> 无需手动 import
Components({
  resolvers: [ElementPlusResolver()],
  dts: "src/components.d.ts"  // 自动生成类型声明
})
```

### Tailwind CSS — 原子化样式
**文件**: `code/frontend/src/views/login/index.vue`

```html
<div class="min-h-screen grid lg:grid-cols-2">
  <div class="relative hidden lg:flex flex-col justify-between ...">
```
- 无需手写大量 CSS 类名
- `lg:grid-cols-2` 响应式断点适配 PC/移动端
- `space-y-5` 间距工具类替代手动 margin

### Vite 代理配置
**文件**: `code/frontend/vite.config.ts`

```typescript
server: {
  port: 8848,
  host: "0.0.0.0",  // 局域网手机可访问
  proxy: {
    "/api": { target: "http://localhost:8080", changeOrigin: true },
    "/login": { target: "http://localhost:8080", changeOrigin: true }
  }
}
```
- 开发时 `/api/auth/login` 自动转发到后端 8080
- 手机访问：通过 CORS 配置支持 `10.*`、`192.168.*` 等局域网 IP

### Pinia — 用户状态管理
**文件**: `code/frontend/src/store/user.ts`

```typescript
export const useUserStore = defineStore("user", () => {
  const loginAction = async (username: string, password: string, captchaUuid: string, captchaCode: string) => {
    const res = await loginApi({ username, password, captchaUuid, captchaCode });
    // 后端返回的 accessToken 存入 localStorage，后续请求通过 axios 拦截器自动携带
    localStorage.setItem("token", data.accessToken);
  };
});
```

### Axios 请求拦截器 — Token 自动续期
**文件**: `code/frontend/src/utils/request.ts`

```typescript
// 请求拦截器：每次请求前自动检查 Token 是否即将过期
request.interceptors.request.use(async (config) => {
  let token = localStorage.getItem("token");
  if (token && isTokenExpiringSoon(token)) {  // 提前 5 分钟检测
    // 调用 /refresh-token 接口自动续期
    const res = await axios.post("/refresh-token", { refreshToken });
    token = res.data.data.accessToken;
    localStorage.setItem("token", token);
  }
  config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

---

## 第 5 页：后端技术选型详解（结合代码）

### Spring Boot 3.4 — 快速启动
```java
@SpringBootApplication
@MapperScan("cn.oa.mapper")  // 扫描所有 Mapper 接口
public class OaApplication {
    public static void main(String[] args) {
        SpringApplication.run(OaApplication.class, args);
    }
}
```

### MyBatis-Plus 3.5.9 — 核心 ORM

**自动填充审计字段** — 文件: `cn.oa.common.config.MyMetaObjectHandler`
```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "createBy", () -> UserContext.get().getUsername(), String.class);
    }
}
// Entity 中声明自动填充
@TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
@TableField(fill = FieldFill.INSERT) private String createBy;
```

**Lambda 查询条件** — 文件: `cn.oa.service.impl.EmployeeServiceImpl`
```java
// 使用 LambdaQueryWrapper 代替字符串字段名，编译期类型安全
return this.lambdaQuery()
    .eq(SysEmployee::getEmpCode, empCode)  // 不会拼写错字段名
    .eq(SysEmployee::getDelFlag, "0")      // 自动过滤逻辑删除
    .one();
```

**乐观锁 @Version** — 用于并发冲突检测
```java
@Version
private Integer version;  // 更新时自动 +1，版本不一致时抛异常
```

### Flyway 数据库迁移
**文件**: `code/backend/oa-web/src/main/resources/db/migration/`

```
V1011__workflow_graph_seeds.sql     // 工作流图结构种子数据
V1012__tiered_workflow_approval_chains.sql  // 分级审批链
V1013__workflow_schema_v3_custom_runtime.sql // Schema v3 运行时
```
- 每次启动自动执行未运行过的脚本
- 通过 `flyway_schema_history` 表追踪已执行的迁移
- 开发环境冲突时：`DROP TABLE flyway_schema_history;` 即可重置

### EasyExcel — 导出命名规范
**文件**: `cn.oa.utils.ExcelExportUtil`

```java
public static <T> void export(HttpServletResponse response, String baseFileName, ...) {
    // 文件名格式：业务名_年月日_时分秒.xlsx
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String finalFileName = baseFileName + "_" + timestamp + ".xlsx";
    response.setHeader("Content-Disposition", "attachment;filename=" + encodedName);
    // ... EasyExcel 写入
}
// 调用方只需：ExcelExportUtil.export(response, "请假数据", LeaveExportVO.class, list);
// 产出：请假数据_20260616_153012.xlsx
```

---

## 第 6 页：Redis 的 6 种使用场景（⭐️ 重点）

### 场景 1：JWT Token 服务端缓存 — 支持主动失效
**文件**: `cn.oa.service.impl.AuthServiceImpl`

```java
// 登录成功后 Token=生成JWT,存入Redis,设置2小时过期
String token = jwtUtil.generateToken(employee.getId(), employee.getEmpName());
redisTemplate.opsForValue().set("token:" + employee.getId(), token, 7200, TimeUnit.SECONDS);
// 缓存角色信息，供后续权限拦截校验
redisTemplate.opsForValue().set("roles:" + employee.getId(), roleKeys, 7200, TimeUnit.SECONDS);

// 管理员强制下线：直接删除 Redis 中的 Token
// 用户退出：同时删除 Token 和角色缓存
public void logout(Long empId) {
    redisTemplate.delete("token:" + empId);
    redisTemplate.delete("roles:" + empId);
}
```
**为什么这么做**：JWT 本身无状态，无法在服务端主动失效。配合 Redis 后，管理员可强制踢人下线。

### 场景 2：登录 IP 限流
**文件**: `cn.oa.common.interceptor.RateLimitInterceptor`

```java
public boolean preHandle(HttpServletRequest request, ...) {
    String ip = IpUtil.getClientIp(request);
    String key = "rate:login:" + ip;
    long count = redisService.increment(key);   // INCR 原子递增
    if (count == 1) {
        redisService.expire(key, 60, TimeUnit.SECONDS);  // 60秒窗口
    }
    if (count > 5) {  // 每分钟最多 5 次
        response.setStatus(429);
        return false;  // 拦截请求
    }
    return true;
}
```
**实现原理**：利用 Redis 的 INCR 原子操作 + EXPIRE 自动过期，无需手动清除计数。

### 场景 3：在线用户管理
**文件**: `cn.oa.service.OnlineUserService`

```java
// 用户登录时记录在线状态到 Redis
redisTemplate.opsForValue().set("online:user:" + empId, userInfo, 30, TimeUnit.MINUTES);
// 管理员查询在线用户：SCAN 全量查询
Set<String> keys = redisTemplate.keys("online:user:*");
// 管理员点击"下线"：直接删除 Redis key
redisTemplate.delete("online:user:" + empId);
```

### 场景 4：图形验证码存储
**文件**: `cn.oa.common.service.RedisService`

```java
// 生成验证码时：uuid → 验证码值, 5分钟过期
redisService.set("captcha:" + uuid, code, 300);
// 登录验证时：取出比对后立即删除（一次性使用）
String stored = redisService.get("captcha:" + uuid);
redisService.delete("captcha:" + uuid);
return stored != null && stored.equalsIgnoreCase(inputCode);
```

### 场景 5：Token 自动续期
**文件**: `cn.oa.service.impl.AuthServiceImpl`

```java
public LoginVO refreshToken(String refreshToken) {
    Claims claims = jwtUtil.parseToken(refreshToken);  // 解析 JWT
    Long empId = claims.get("empId", Long.class);
    // 查 Redis 确认 Token 仍有效（未被管理员踢下线）
    Object storedToken = redisTemplate.opsForValue().get("token:" + empId);
    if (storedToken == null || !storedToken.equals(refreshToken)) {
        throw new BusinessException("refreshToken 已失效，请重新登录");
    }
    // 生成新 Token，重置 Redis 过期时间
    String newToken = jwtUtil.generateToken(empId, empName);
    redisTemplate.opsForValue().set("token:" + empId, newToken, 7200, TimeUnit.SECONDS);
    return newToken;
}
```

### 场景 6：Spring Event 跨模块消息传递
**文件**: `cn.oa.service.impl.AuthServiceImpl`

```java
// 借助 Spring Event 在业务模块间传递事件（如审批完成→扣减假期）
// Redis 只做缓存，事件通信使用 Spring 内置事件机制
applicationEventPublisher.publishEvent(new ApprovalCompletedEvent(businessId, businessType));
```

---

## 第 7 页：JWT 认证体系详解

### JWT Token 结构
```
Header:  { "alg": "HS256" }
Payload: { "empId": 1, "empName": "张三", "iat": 1718000000, "exp": 1718007200 }
Signature: HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
```

### Token 生成
**文件**: `cn.oa.common.utils.JwtUtil`

```java
// 使用 jjwt 0.12.5 库，HMAC-SHA256 签名
public String generateToken(Long empId, String empName) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + 7200 * 1000);  // 2小时后过期
    return Jwts.builder()
        .claim("empId", empId)
        .claim("empName", empName)
        .issuedAt(now)
        .expiration(expiration)
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
}
```

### 拦截器校验流程
**文件**: `cn.oa.common.interceptor.AuthInterceptor`

```java
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    // 1. 从请求头获取 Token
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) { ... }

    // 2. 解析 JWT，获取用户信息
    Claims claims = jwtUtil.parseToken(token.replace("Bearer ", ""));

    // 3. 检查 Redis 中 Token 是否有效（防止管理员踢人后仍能用旧 Token）
    Object redisToken = redisTemplate.opsForValue().get("token:" + empId);
    if (!token.equals(redisToken)) { throw new AuthException("Token 已失效"); }

    // 4. 检查角色权限（如有 @RequirePermission 注解）
    if (handler instanceof HandlerMethod) {
        RequirePermission ann = ((HandlerMethod) handler).getMethodAnnotation(RequirePermission.class);
        if (ann != null) {
            Set<String> perms = getPermissions(empId);  // 从 Redis 获取
            if (!perms.contains(ann.value())) { throw new AuthException("无权限"); }
        }
    }
    return true;
}
```

### 前端路由守卫配合
**文件**: `code/frontend/src/router/index.ts`

```typescript
router.beforeEach((to, _from, next) => {
    if (to.path === "/login" || to.path === "/register" || to.path === "/forgot-password") {
        next(); return;  // 公开路由
    }
    const token = localStorage.getItem("token");
    if (!token) { next("/login"); return; }  // 无 Token 跳登录
    // 解析 JWT Payload 检查是否过期（前端先检测，免发无效请求）
    const payload = JSON.parse(atob(token.split(".")[1]));
    if (payload.exp && Date.now() / 1000 > payload.exp) {
        localStorage.removeItem("token"); next("/login"); return;
    }
    next();
});
```

---

## 第 8 页：自研工作流引擎深度解析（⭐️ 核心亮点）

### 为什么自研而非使用 Flowable/Camunda？
| 方案 | 复杂度 | 部署 | 定制成本 |
|------|--------|------|----------|
| Flowable | ⭐⭐⭐⭐⭐ | 需额外部署流程引擎服务 | 学习曲线陡峭 |
| **自研方案** | ⭐⭐ | 随业务一起部署，零依赖 | 根据业务定制，灵活可控 |

### 流程定义数据结构（JSON Schema v2）
**文件**: `wf_definition` 表的 `process_def` 字段

```json
{
  "schemaVersion": 2,
  "nodes": [
    { "id": "start", "type": "start", "label": "开始" },
    { "id": "approve1", "type": "approval", "label": "直属上级审批",
      "assignee": { "type": "role_chain", "role": "DEPT_MANAGER", "level": 1 },
      "timeout": { "hours": 48, "action": "escalate", "escalateTo": "approve2" }
    },
    { "id": "approve2", "type": "approval", "label": "总监审批",
      "condition": { "field": "amount", "operator": ">", "value": 5000 },
      "assignee": { "type": "role", "role": "DIRECTOR" }
    },
    { "id": "end", "type": "end", "label": "结束" }
  ],
  "edges": [
    { "from": "start", "to": "approve1" },
    { "from": "approve1", "to": "approve2", "conditionExpression": "amount > 5000" },
    { "from": "approve1", "to": "end", "conditionExpression": "amount <= 5000" },
    { "from": "approve2", "to": "end" }
  ]
}
```

### 4 维分级路由实现
**文件**: `cn.oa.service.workflow.WorkflowRouteService`

```java
// 根据请求的金额、天数、小时数、角色级别自动选择审批节点
public List<Node> resolveRoute(Map<String, Object> variables, List<Node> nodes) {
    BigDecimal amount = (BigDecimal) variables.get("amount");
    Integer days = (Integer) variables.get("days");
    Integer hours = (Integer) variables.get("hours");
    Integer roleLevel = (Integer) variables.get("role_level");
    // 遍历节点，匹配条件表达式
    return nodes.stream()
        .filter(node -> evaluateConditions(node, amount, days, hours, roleLevel))
        .collect(Collectors.toList());
}
```

### 动态审批人解析
**文件**: `cn.oa.service.workflow.ApproverResolver`

```java
// role_chain 实现：找发起人的直属上级
public Long resolveApprover(Long initiatorId, String role, int level) {
    SysEmployee initiator = employeeService.getById(initiatorId);
    // 查部门负责人 → 总监 → 总经理，逐级上升
    List<Long> chain = deptService.getLeaderChain(initiator.getDeptId());
    return chain.size() > level ? chain.get(level) : chain.get(chain.size() - 1);
}
```

### Spring Event 业务回调
**文件**: `cn.oa.service.impl.AuthServiceImpl`（以请假审批通过为例）

```java
// 工作流审批完成后，发布事件
applicationEventPublisher.publishEvent(new WorkflowCompletedEvent(instanceId, businessType, businessId, status));

// 请假业务监听器
@EventListener
@Transactional
public void onLeaveApproved(WorkflowCompletedEvent event) {
    if ("leave".equals(event.getBusinessType()) && "approved".equals(event.getStatus())) {
        Leave leave = leaveService.getById(event.getBusinessId());
        // 自动扣减假期余额
        leaveBalanceService.deduct(leave.getEmployeeId(), leave.getType(), leave.getDays());
    }
}
```

---

## 第 9 页：数据库设计与索引优化

### 核心表结构

**sys_employee（员工表）**
```sql
CREATE TABLE sys_employee (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,    -- 自增主键
  emp_code    VARCHAR(32)  NOT NULL,                 -- 员工编号（登录名）
  emp_name    VARCHAR(64)  NOT NULL,                 -- 员工姓名
  password    VARCHAR(256) NOT NULL,                 -- BCrypt 加密密码
  phone       VARCHAR(16),                           -- 手机号
  email       VARCHAR(128),                          -- 邮箱
  dept_id     BIGINT,                                -- 所属部门
  status      INT DEFAULT 1,                         -- 状态 1=启用 0=禁用
  del_flag    VARCHAR(2) DEFAULT '0',                -- 逻辑删除
  create_time DATETIME, update_time DATETIME,        -- 审计字段
  INDEX idx_dept (dept_id),
  UNIQUE INDEX idx_emp_code (emp_code)
);
```

**wf_task（工作流任务表）**
```sql
CREATE TABLE wf_task (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  instance_id BIGINT NOT NULL,           -- 流程实例 ID
  node_id     VARCHAR(64),               -- 当前节点 ID
  assignee    BIGINT,                    -- 审批人
  status      VARCHAR(8) DEFAULT '0',     -- 0=待处理 1=通过 2=拒绝
  create_time DATETIME,
  action_time DATETIME,                  -- 处理时间
  remark      VARCHAR(1024),             -- 审批意见
  INDEX idx_instance (instance_id),
  INDEX idx_assignee (assignee),
  INDEX idx_status (status)
);
```

### 索引设计原则
- **高频查询字段**：外键（dept_id、assignee）和状态字段（status）建索引
- **联合索引**：`(dept_id, status)` 用于部门考勤统计
- **唯一索引**：`emp_code`、`phone` 确保数据一致性

---

## 第 10 页：统一响应与全局异常处理

### 统一响应格式 R<T>
**文件**: `cn.oa.common.result.R`

```java
@Data
public class R<T> {
    private int code;       // 0=成功，非0=错误码
    private String message; // 操作提示
    private T data;         // 业务数据

    public static <T> R<T> ok(T data) {
        return new R<>(0, "操作成功", data);
    }
    public static <T> R<T> fail(String message) {
        return new R<>(-1, message, null);
    }
}
```
**前端统一处理**：axios 响应拦截器判断 `res.code === 0`，非0显示 `res.message`

### 全局异常处理
**文件**: `cn.oa.common.exception.GlobalExceptionHandler`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e) {
        return R.fail(e.getMessage());   // 业务异常 → 返回错误消息
    }
    @ExceptionHandler(AuthException.class)
    public R<Void> handleAuth(AuthException e) {
        return R.error(401, e.getMessage());  // 认证异常 → 401 状态码
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        return R.fail(msg);  // 参数校验失败 → 返回具体错误
    }
}
```

---

## 第 11 页：前端 ECharts 数据看板实现详解

### 图表初始化流程
**文件**: `code/frontend/src/views/oa/dashboard/index.vue`

```typescript
// 1. 页面加载时请求后端数据
async function fetchDashboardData() {
    const response = await getDashboardStats("today");
    dashboardData.value = response.data;
    await nextTick();
    initCharts();  // 等 DOM 渲染完成后初始化图表
}

// 2. 创建 ECharts 实例
function createChart(element: HTMLDivElement | undefined) {
    if (!element) return undefined;
    const chart = echarts.init(element);
    charts.push(chart);  // 收集所有图表实例，用于统一 resize
    return chart;
}

// 3. 双 Y 轴趋势图（活跃员工柱状图 + 有效工时折线图）
function initDailyTrendChart() {
    const data = dashboardData.value.officeActivityTrend;
    chart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { top: 0, right: 0 },
        yAxis: [
            { type: "value", name: "员工数", minInterval: 1 },  // 左轴
            { type: "value", name: "工时(h)" }                   // 右轴
        ],
        series: [
            {
                name: "活跃员工",
                type: "bar",  // 柱状图
                itemStyle: { color: createGradient("#facc15", "#f59e0b") }  // 黄色渐变
            },
            {
                name: "有效工时",
                type: "line",  // 折线图
                yAxisIndex: 1,  // 绑定右轴
                smooth: true
            }
        ]
    });
}

// 4. 响应式：窗口变化时所有图表自适应
window.addEventListener("resize", () => charts.forEach(chart => chart.resize()));
```

### 图表主题工具
**文件**: `code/frontend/src/utils/chartTheme.ts`

```typescript
export function createGradient(start: string, end: string, horizontal = false) {
    return new echarts.graphic.LinearGradient(0, 0, horizontal ? 1 : 0, horizontal ? 0 : 1, [
        { offset: 0, color: start },
        { offset: 1, color: end }
    ]);
}
// 使用：柱状图渐变色、面积图渐变填充都复用此函数
```

---

## 第 12 页：核心功能模块详解（1）— 登录与注册

### 图形验证码实现
**文件**: `cn.oa.common.utils.CaptchaUtil`

```java
public static CaptchaResult generate(RedisService redisService) {
    // 生成 4 位随机数字验证码
    String code = String.format("%04d", (int)(Math.random() * 10000));
    String uuid = UUID.randomUUID().toString().replace("-", "");
    // 存入 Redis，5 分钟过期
    redisService.set("captcha:" + uuid, code, 300);
    // 返回 Base64 图片和 uuid（前端提交登录时带回 uuid+code 给后端验证）
    return new CaptchaResult(uuid, generateBase64Image(code));
}
```

### 登录表单前端交互（眼球动画）
**文件**: `code/frontend/src/components/EyeBall.vue`

```typescript
// 眼球组件：鼠标移动时计算瞳孔位置
const pupilPosition = computed(() => {
    const eye = eyeRef.value.getBoundingClientRect();
    const deltaX = mouseX.value - (eye.left + eye.width / 2);
    const deltaY = mouseY.value - (eye.top + eye.height / 2);
    const distance = Math.min(Math.sqrt(deltaX ** 2 + deltaY ** 2), props.maxDistance);
    const angle = Math.atan2(deltaY, deltaX);
    return {
        x: Math.cos(angle) * distance,
        y: Math.sin(angle) * distance
    };
});
```

**文件**: `code/frontend/src/views/login/index.vue`

- 4 个角色（Purple、Black、Orange、Yellow）分别定义鼠标跟踪逻辑
- 输入密码时：Purple 角色会"偷看"（`isPurplePeeking` 状态）
- 输入用户名时：Purple 和 Black 角色相互对视
- 随机眨眼 + 身体倾斜模拟生动效果

### 登录请求链路
```typescript
// 前端
const handleLogin = async () => {
    loading.value = true;
    await userStore.loginAction(username, password, captchaUuid, captchaCode);
    router.push("/welcome");
};

// 后端 AuthService.login()
LoginVO vo = login(username, password, request);
// 1. 查员工表 → BCrypt 比对密码
// 2. 查角色权限 → 生成 JWT
// 3. Token + 角色信息存入 Redis
// 4. 记录登录日志（IP、浏览器、操作时间）
```

---

## 第 13 页：核心功能模块详解（2）— 权限与考勤

### RBAC 权限模型实现

**数据库关系**：
```
sys_employee ── sys_emp_role ── sys_role ── sys_role_menu ── sys_menu
     ↓                                            ↓
  员工ID                                      菜单/权限码
```

**注解式接口鉴权**：
```java
@RestController
public class LeaveController {

    @GetMapping("/api/v1/hr/leaves")
    @RequirePermission("hr:leave:list")    // 需要此权限码才能访问
    public R<PageResult<Leave>> list(...) { ... }

    @PostMapping("/api/v1/hr/leaves")
    @RequirePermission("hr:leave:create")  // 不同的操作需要不同的权限码
    public R<Void> create(...) { ... }
}
```

**角色级别设计**：
```java
// AuthInterceptor 中按角色级别访问控制
ADMIN        → 所有功能
DEPT_MANAGER → 部门内管理（考勤、审批等）
TEAM_LEAD    → 团队级审批权
USER         → 基础功能（打卡、请假申请、查看公告）
```

### 考勤打卡实现

**文件**: `cn.oa.controller.AttendanceController`

```java
@PostMapping("/api/v1/oa/attendance/clock-in")
@RequirePermission("attendance:checkin")
public R<Void> clockIn(HttpServletRequest request) {
    Long empId = WebUtil.getEmpId(request);
    LocalDate today = LocalDate.now();
    // 查今日是否已打卡（防止重复打卡）
    AttendanceRecord record = attendanceService.getTodayRecord(empId, today);
    if (record != null) return R.fail("今日已打卡");
    // 判断是否迟到（超过考勤组规定的上班时间）
    boolean isLate = attendanceService.isLate(empId, LocalTime.now());
    attendanceService.createRecord(empId, today, LocalTime.now(), isLate ? "late" : "normal");
    return R.ok();
}
```

---

## 第 14 页：前端动画登录页实现详解

### 动画角色系统

**文件**: `code/frontend/src/components/Pupil.vue`  
**文件**: `code/frontend/src/components/EyeBall.vue`  

```
鼠标跟踪原理：
  1. window.addEventListener("mousemove") 监听鼠标位置
  2. 计算鼠标位置相对于眼球的偏移量 (deltaX, deltaY)
  3. 限制最大移动距离 (maxDistance=5/10px)
  4. 用 transform: translate(x, y) 定位瞳孔
```

**角色交互逻辑**：
```typescript
// 输入密码时 Purple 偷看
watch(() => [loginForm.password, showPassword.value], () => {
    if (loginForm.password.length > 0 && showPassword.value) {
        // 随机 2-5 秒后 Purple 偷看 800ms
        setTimeout(() => { isPurplePeeking.value = true; ... }, random(2000, 5000));
    }
});

// 输入用户名时角色对视
watch(isTyping, (newVal) => {
    if (newVal) {
        isLookingAtEachOther.value = true;
        setTimeout(() => { isLookingAtEachOther.value = false; }, 800);
    }
});
```

### 页面左右分栏布局
```
左侧（灰白渐变）               右侧（深黑背景）
┌────────────────────┐  ┌────────────────────┐
│ OA办公系统          │  │  欢迎回来！          │
│                    │  │  请输入您的登录信息    │
│   [动画角色区域]    │  │                     │
│   Purple Black     │  │  用户名 [________]  │
│   Orange Yellow    │  │  密码   [___👁️___]  │
│   (跟随鼠标运动)    │  │  验证码 [_] [图片]  │
│                    │  │                     │
│ 企业协同管理平台      │  │   [ 登 录 ]        │
└────────────────────┘  │  还没有账号？注册      │
                        └────────────────────┘
```

---

## 第 15 页：通知与 WebSocket 实时推送

### WebSocket 配置
**文件**: `cn.oa.config.WebSocketConfig`

```java
@Configuration
public class WebSocketConfig {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
```

### 消息推送流程
```
后端审批完成 → publish Spring Event
  → WebSocket 处理器获取事件 → 推送给指定用户
    → 前端 WebSocket 收到消息 → 更新未读计数 + 弹窗提醒
```

**三种通知渠道对比**：

| 渠道 | 实时性 | 持久性 | 适用场景 |
|------|--------|--------|----------|
| WebSocket | ⭐⭐⭐ 即时 | ❌ 不持久 | 审批结果推送、新消息提醒 |
| 消息表 `oa_message` | ⭐⭐ | ✅ 持久化 | 内信件、离线消息 |
| 待办表 `oa_todo` | ⭐⭐ | ✅ 持久化 | 待办任务、系统通知 |

---

## 第 16 页：Excel 多维度导出实现

### 导出工具类
**文件**: `cn.oa.utils.ExcelExportUtil`

```java
public static <T> void export(HttpServletResponse response, String baseFileName, Class<T> head, List<T> data) {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    // 命名规范：业务名_年月日_时分秒.xlsx
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String finalFileName = baseFileName + "_" + timestamp;
    response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(finalFileName, "UTF-8") + ".xlsx");
    EasyExcel.write(response.getOutputStream(), head)
        .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())  // 自动列宽
        .sheet(baseFileName)
        .doWrite(data);
}
```

### 支持的导出功能

| 导出模块 | 后端 Controller | 文件名示例 |
|----------|-----------------|-----------|
| 考勤数据 | AttendanceController | `考勤数据_20260616_153012.xlsx` |
| 请假记录 | LeaveApplyController | `请假数据_20260616_153012.xlsx` |
| 出差记录 | BusinessTripController | `出差数据_20260616_153012.xlsx` |
| 经费报销 | ExpenseController | `经费数据_20260616_153012.xlsx` |
| 采购申请 | PurchaseController | `采购数据_20260616_153012.xlsx` |
| 借支记录 | LoanController | `借支数据_20260616_153012.xlsx` |
| 加班记录 | OvertimeController | `加班数据_20260616_153012.xlsx` |
| 外出记录 | OutingController | `外出数据_20260616_153012.xlsx` |

---

## 第 17 页：CORS 跨域与手机局域网访问

### 服务端 CORS 配置
**文件**: `cn.oa.common.config.WebMvcConfig`

```java
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOriginPatterns(
            "http://localhost:*",         // 本机开发
            "http://127.0.0.1:*",         // 本机 IPv4
            "http://10.*:*",              // 公司/校园网（A类网段）
            "http://192.168.*:*",         // 家庭路由器（C类网段）
            "http://172.*:*"              // 企业内网（B类网段）
        )
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)   // 允许携带 Cookie/Authorization 头
        .maxAge(3600);            // 预检请求缓存 1 小时
}
```
**为什么需要这个**：Spring 6 严格校验 Origin 头，`localhost` 和 `10.x.x.x` 在浏览器看来是不同的来源，不加白名单则跨域请求会被拒绝。

### 前端 Vite 开发代理
```typescript
server: {
    host: "0.0.0.0",  // 绑定所有网络接口
    proxy: {
        "/api": { target: "http://localhost:8080", changeOrigin: true },
        "/login": { target: "http://localhost:8080", changeOrigin: true }
    }
}
```

---

## 第 18 页：GitHub Actions CI 流水线

### CI 文件结构
```
.github/workflows/
├── backend.yml      # 后端构建 + 测试
├── frontend.yml     # 前端构建 + 类型检查
├── api-test.yml     # API 集成测试
└── security-scan.yml # 安全扫描
```

### backend.yml 核心配置
```yaml
name: Backend CI
on:
  push:
    paths: ["code/backend/**"]  # 只有后端代码变更时才触发
jobs:
  build:
    runs-on: ubuntu-latest
    services:
      mysql:  # CI 环境启动 MySQL 容器
        image: mysql:8
          options: --health-cmd="mysqladmin ping" ...
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - name: Build and Test
        run: mvn -B test
```

---

## 第 19 页：项目亮点与创新点总结

### ⭐️ 技术亮点 TOP 5

1. **自研工作流引擎**：JSON 图结构 + 4 维条件路由 + 动态审批人 + Spring Event 回调，支撑 7+ 种审批业务
2. **Redis 多场景复用**：Token 管理、IP 限流、在线用户、验证码、角色缓存，一套缓存中间件发挥 5 种核心作用
3. **JWT + Redis 双重认证**：JWT 验证身份 + Redis 存储支持服务端主动失效，兼顾无状态和可控性
4. **ECharts 数据看板**：11 种图表（趋势图、热力图、散点图、雷达图、仪表盘）直观展示运营数据
5. **眼球追踪动画登录页**：基于鼠标坐标计算和 Vue 响应式动画的交互设计

### 🏆 工程亮点
- **5 模块分层架构**：按功能分层非业务分模块，迭代效率高
- **Flyway 数据库迁移**：版本化 SQL 脚本，团队协作无冲突
- **Excel 导出命名规范**：统一 `文件名_年月日_时分秒.xlsx`
- **全局异常处理**：`@RestControllerAdvice` 统一异常→统一响应格式
- **AOP 操作日志**：`@OperationLog` 注解零侵入记录所有核心操作

---

## 第 20 页：项目总结

### 项目成果
- ✅ 完成 8 大业务域、60+ 功能模块
- ✅ 自研工作流引擎，7+ 种审批业务统一接入
- ✅ 前后端分离、5 模块分层架构、TypeScript 全覆盖
- ✅ RBAC 权限模型 + 注解鉴权 + 操作日志 AOP
- ✅ 实时 WebSocket 通知 + ECharts 数据看板
- ✅ 手机局域网可访问、Excel 规范导出

### 技术收获
- **Spring Boot 3.4 + MyBatis-Plus 3.5.9**：事务管理、乐观锁、自动填充
- **Vue 3 Composition API + TypeScript**：响应式编程、类型安全
- **工作流引擎设计**：图结构定义、条件路由、事件驱动回调
- **认证与权限体系**：JWT + Redis + 拦截器 + 注解鉴权的完整链路

### 适用场景
- 中小型企业 OA 系统 / 毕业设计 / 课程设计 / 内部管理系统原型

---

## 第 21 页：致谢

感谢各位老师的指导与帮助！

欢迎提问与建议 🙏
