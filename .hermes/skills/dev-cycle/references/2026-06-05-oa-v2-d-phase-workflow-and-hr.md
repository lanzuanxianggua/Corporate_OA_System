# D 阶段实战：流程引擎 e2e + oa-hr-leave 从零建

**日期**: 2026-06-05
**分支**: v2-platform
**HEAD**: e4def5c (4 commit, 12 修复入仓)

---

## 1. D 阶段范围

D 阶段从 oa-system e2e 鉴权全链路打通（commit `fcb4b98` + `db72ef3` + `aa15410`）后，进入**流程引擎 + 业务模块实战**。

**两个子目标**：
- **D-1**: 验证 `oa-workflow` 模块的 `WfInstanceController.start` 端到端（流程启动 → 创建 wf_instance + wf_task）
- **D-2**: 从 0 建 `oa-hr-leave` 业务模块（HrLeave entity + service + controller + V930 SQL），验证与 workflow 集成

**为什么需要 D-1**：oa-system 鉴权链通了，但 workflow 模块是否真能跑通未知——可能有 1+ 个 e2e 鉴权链 4 轮错配同类问题没暴露。

**为什么需要 D-2**：v2 计划 18 Maven 模块（13 业务 + 5 平台层），oa-hr-leave 是第一个从 0 建的业务模块，作为模板。

---

## 2. D-1: WfInstanceController.start 端到端

### 2.1 启动链路

`POST /api/v1/workflow/instances/start {defKey: "hr_leave"}` 走通：

1. `JwtAuthenticationFilter` 解析 token → 查 `sys_employee.username=admin` + `sys_emp_role` 取 role_id=1 → 查 `sys_role_permission` 取 perm_codes (10 hr-leave + 7 workflow)
2. `PermissionInterceptor` preHandle 校验 `workflow:instance:start` perm in token
3. `WfInstanceController.start(defKey)` → `WfInstanceService.start(...)` → `WfEngine.startProcess(...)`
4. WfEngine: 查 `wf_definitions` by defKey → 查 `wf_transitions` by fromNodeId (start_node) → 创建 `wf_instance` + `wf_task` (assignee 暂 null)

### 2.2 4 轮 e2e 错配（oa-workflow 同类问题）

**第 1 轮**：缺 `oa-workflow` 依赖
```xml
<!-- oa-platform-web/pom.xml 必须加 -->
<dependency>
  <groupId>cn.oa</groupId>
  <artifactId>oa-workflow</artifactId>
</dependency>
```
否则 `WfInstanceController` 在 fat jar `BOOT-INF/lib/oa-workflow.jar` 内但 web 没引 → Spring 扫不到。

**第 2 轮**：V900 seed 0 条 workflow 权限
```sql
-- V920__workflow_permissions.sql
INSERT INTO sys_permission (perm_code, perm_name, perm_type, status, create_by) VALUES
  ('workflow:instance:start', '启动流程', 'BUTTON', 'ACTIVE', 'system'),
  ('workflow:instance:view', '查看流程', 'BUTTON', 'ACTIVE', 'system'),
  ('workflow:task:view', '查看任务', 'BUTTON', 'ACTIVE', 'system'),
  ('workflow:task:approve', '审批任务', 'BUTTON', 'ACTIVE', 'system');
INSERT INTO sys_role_permission (role_id, perm_id, create_by)
  SELECT 1, id, 'system' FROM sys_permission WHERE perm_code LIKE 'workflow%' AND del_flag = '0';
```

**第 3 轮**：Mapper 多参缺 `@Param`
```java
// 错
@Select("SELECT * FROM wf_transitions WHERE from_node_id = #{fromNodeId} AND action = #{action}")
List<WfTransition> findByFromNodeAndAction(Long fromNodeId, String action);
// 启动: Parameter 'fromNodeId' not found

// 对
List<WfTransition> findByFromNodeAndAction(
    @Param("fromNodeId") Long fromNodeId, 
    @Param("action") String action);
```

`WfTransitionMapper` 2 方法 + `WfDelegationMapper` 3 方法全部加 `@Param`。

**第 4 轮**：WfInstance 业务校验重复
```java
// start 后立刻二次 start 同一 businessKey
// 业务校验: "该业务单据已存在进行中的流程: LEAVE_2026_0001"
// HTTP 200, code=1, success=true (业务层拒绝, 但 HTTP 成功)
```

---

## 3. D-2: oa-hr-leave 从零建 6 文件

### 3.1 模块结构

```
code/backend/oa-hr-leave/
├── pom.xml
└── src/main/java/cn/oa/hr/leave/
    ├── entity/HrLeave.java
    ├── mapper/HrLeaveMapper.java
    ├── service/HrLeaveService.java
    └── controller/HrLeaveController.java

code/backend/oa-platform-web/
└── src/main/resources/db/migration/
    └── V930__hr_leave_tables.sql
```

### 3.2 文件要点

**HrLeave.java** (entity)：
- 继承 `BaseEntity` (4 字段自动填充)
- 8 业务字段: leaveType / startDate / endDate / reason / status / empId / empName / deptName
- 1 关联字段 `wfInstanceId` 关联 `wf_instance.id`

**HrLeaveMapper.java**：
- 继承 `BaseMapper<HrLeave>`
- 1 自定义 `@Select` 关联 `sys_employee` (拿 username + dept_id → `sys_dept` 拿 dept_name)

**HrLeaveService.java**：
- `submit(HrLeaveDTO)` `@Transactional` 事务: 1) INSERT hr_leave 2) `wfInstanceService.start("hr_leave", businessKey="LEAVE_2026_0001")` 3) UPDATE hr_leave.wf_instance_id
- `findById(id)` 普通 SELECT

**HrLeaveController.java**：
- `POST /api/v1/hr/leaves` `@RequirePermission("hr:leave:submit")` 调 service.submit
- `GET /api/v1/hr/leaves/mine` `@RequirePermission("hr:leave:view")` 查 `UserContext.get().getEmpId()` 列表
- `GET /api/v1/hr/leaves/{id}` `@RequirePermission("hr:leave:view")` 详情

**V930__hr_leave_tables.sql**：
```sql
CREATE TABLE hr_leave (
  id BIGINT NOT NULL AUTO_INCREMENT,
  leave_type VARCHAR(20) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  reason VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  emp_id BIGINT NOT NULL,
  emp_name VARCHAR(50),
  dept_name VARCHAR(50),
  wf_instance_id BIGINT,
  del_flag CHAR(1) NOT NULL DEFAULT '0',
  create_by VARCHAR(50),
  create_time DATETIME,
  update_by VARCHAR(50),
  update_time DATETIME,
  PRIMARY KEY (id),
  KEY idx_emp_id (emp_id),
  KEY idx_wf_instance_id (wf_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 业务权限
INSERT INTO sys_permission (perm_code, perm_name, perm_type, status, create_by) VALUES
  ('hr:leave:submit', '提交请假', 'BUTTON', 'ACTIVE', 'system'),
  ('hr:leave:view', '查看请假', 'BUTTON', 'ACTIVE', 'system');
INSERT INTO sys_role_permission (role_id, perm_id, create_by)
  SELECT 1, id, 'system' FROM sys_permission WHERE perm_code LIKE 'hr:leave%' AND del_flag = '0';
```

### 3.3 5 个实战坑（详情见 SKILL.md 5 段）

1. **fat jar `mvn -pl ... -am package` 不打项目内 module** — 必须 `mvn -pl oa-hr-leave -am install` 先推 m2 repo
2. **`@MapperScan` 必须加新 mapper 包** — `cn.oa.hr.leave.mapper`
3. **`/tmp` git-bash = `C:/Users/xiaohuang/AppData/Local/Temp`** — token 文件落盘路径
4. **bash `$(...)` + `***` redact 嵌套** — token 二次 redact 触发 subshell 嵌套错
5. **`POST start 走通 vs GET 10001` 矛盾** — `JwtUtil` 实例化 + `@ConfigurationProperties` 注入不一致

---

## 4. 收官报告核心数据

**4 commit 入仓 v2-platform**：
| SHA | 范围 | 文件数 | +行/-行 |
|-----|------|--------|---------|
| `fcb4b98` | SQL: V200/V910 + 删 sql/ 归档 | 4 | +157/-568 |
| `db72ef3` | oa-system: entity/mapper/MyMetaObjectHandler | 6 | +40/-4 |
| `aa15410` | infra: yml + lombok 3 模块 | 5 | +279/-260 |
| `e4def5c` | workflow: pom dep + V920 + 2 mapper @Param | 4 | +33/-3 |

**19 修复明细** (D-1 之前 15 + D-1 4)：
- **SQL**: V200 del_flag CHAR(1) + V910 password + V920 workflow 权限
- **entity**: 3 @TableName 复数化 + 3 @TableField 错配
- **mapper**: 4 @Param 多参 + 2 表名 + 2 del_flag 错配
- **其他**: MyMetaObjectHandler + 3 pom lombok + yml 127.0.0.1 + yml profile 隔离 + oa-platform-web 加 2 业务 module dep

**6 端点状态**：
| 端点 | 状态 | 备注 |
|------|------|------|
| GET /api/ping | ✓ 200 | public |
| POST /api/auth/login | ✓ 200 + JWT | 11 hr-leave + 7 workflow perms |
| GET /api/auth/me | ✓ 200 (业务权限拦截 system:user:view 缺) | 预期 |
| POST /api/v1/workflow/instances/start | ✓ 200 data=1 | wf_instance id=1 |
| GET /api/v1/workflow/instances/1 | ⚠ 10001 (待修 JwtUtil 实例化) | D 阶段已知 |
| POST /api/v1/hr/leaves | ⚠ 10001 (同根因) | D-2 编译通过 |

**12 业务模块状态**：
- 已建实体: oa-system (5 entity) + oa-workflow (8 entity) + oa-hr-leave (1 entity) = 14 entity
- 待建: oa-hr-employee / oa-hr-attendance / oa-hr-performance / oa-hr-recruitment / oa-hr-training / oa-admin / oa-document / oa-finance / oa-knowledge / oa-message / oa-meeting / oa-task = 12 业务模块 (含 5 hr 子模块)

---

## 5. 下一步

1. **修 JwtUtil 实例化 bug** (5 行, 让 GET 端点 10001 修好)
2. **commit oa-hr-leave 8 文件** (即使 e2e GET 10001, 模块本身可入仓)
3. **建 oa-hr-employee** (下一个 hr 子模块, 5 文件模板同上)
4. **建 oa-finance** (财务模块, 复杂业务, 8+ 文件)
