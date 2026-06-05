# OA v2 项目级硬规则（2026-06-04 摸底更新版）

## 1. lombok 允许使用（2026-06-05 验证反转）
- lombok 1.18.34 + JDK 17 + 中文 Windows + 父 pom `<annotationProcessorPaths>` → **javap 验证 @Getter/@Setter/@Slf4j/@ToString 全部生成代码**
- **v2 项目允许使用 @Getter/@Setter/@Slf4j/@Builder/@ToString 等 Lombok 注解**
- 也允许手写 getter/setter/equals/hashCode/toString/构造器，以各模块实际代码为准
- 2026-06-04 早期 "lombok 禁用" 结论已证明错误（可能是当时环境不同）

## 2. 异常体系
- **优先 `BizException(RCode, String)`** —— 业务异常首选
- **`AuthException` 有两种构造器**（2026-06-04 修正，原 memory 错）：
  - `new AuthException()` → super(RCode.UNAUTHORIZED)，无消息
  - `new AuthException(String msg)` → super(RCode.UNAUTHORIZED, msg)
- `ForbiddenException` / `NotFoundException` / `ParamException` 同样映射各自 RCode
- **要带错误码时用 BizException，不用 AuthException**

## 3. MyBatis-Plus 3.5.9 类存在性（LocalRepo 验证过）
- ✓ `OptimisticLockerInnerInterceptor`（in inner 子包）
- ✗ `BlockAttackInnerInterceptor`（**不存在**）
- ✗ `PaginationInnerInterceptor`（**不存在**）
- 要分页拦截器按需在业务模块单独配 `MybatisPlusInterceptor + PaginationInnerInterceptor`

## 4. 注解位置
- `@RequirePermission` / `@RequireRole` / `@DataScope` 在 `cn.oa.platform.security.annotation`（oa-platform-security 模块）
- **不在 oa-platform-common**

## 5. 父 pom 配置
- 用 `<build><plugins>`（**不是 `<pluginManagement>`**）才能让子模块继承 maven-compiler-plugin 注解处理器配置
- 当前父 pom pluginManagement 出现 0 次，✓

## 6. 模块迁移状态（2026-06-04 摸底）
- oa-common 目录在主仓**已不存在**（只剩 `.claude/worktrees/` 下的 v1 尸体）
- security 类（AuthInterceptor/JwtUtil/RateLimitInterceptor/UserContext/GlobalExceptionHandler/权限注解）**已迁到 oa-platform-security/cn/oa/platform/security/**
- **不要按 CLAUDE.md 路径写死**（CLAUDE.md 描述目标架构，代码已落地）
- oa-hr-leave 和 oa-hr-employee 已有骨架代码（Entity/Mapper/Service/Controller 各 1 套），缺 DTO/VO/测试
- oa-hr-attendance/performance/recruitment/training + oa-admin/document/finance/knowledge/message/meeting/task = 11 个模块仍为空壳

## 7. oa-hr 双 HrLeaveService
- `cn.oa.hr.service.HrLeaveService`（Controller 入口）
- `cn.oa.hr.service.leave.HrLeaveService`（工作流回调入口）
- 是项目分层模式（与 finance module service/ vs service/v1/ 同 pattern）
- **T11 不能合并这两份** —— 否则工作流回调会断
- 未来看到 service 目录有同名接口 + 子包同名接口，先 grep import 决定哪份 active，不要直接合并

## 8. Sibling subagent 实际进度
- 4 个 platform-* + oa-system + oa-workflow = **6 个模块共 80+ java 文件已写完**
- 17/17 测试通过：common 6 + security 4 + web 3 + system 4
- 8 维度审计全过：lombok/异常/注解位置/pom 插件/MP 类/旧 cn.oa.common 引用/无参构造器/迁出
- **全部 untracked，零 commit** —— 按模块分 6 commit 入仓，不要一锅端

## 验证命令
```bash
# 编译 + 测试
cd E:/JavaProject/Corporate_OA_System/code/backend
mvn -pl oa-platform-common,oa-platform-security,oa-platform-web,oa-system -am -DskipTests=false test
# 8 维度审计
cd E:/JavaProject/Corporate_OA_System
grep -rn "import lombok" code/backend --include="*.java"
grep -rn "@Slf4j\|@Getter\|@Setter\|@Data\|@Builder" code/backend --include="*.java"
grep -rn "cn\.oa\.common\." code/backend --include="*.java"
grep -rn "BlockAttackInnerInterceptor\|PaginationInnerInterceptor" code/backend --include="*.java"
grep -c "pluginManagement" code/backend/pom.xml
```
