# 05-modules/05-platform-common - 平台层模块详细设计

> 版本: v2.0-draft
> 日期: 2026-06-04
> 状态: **Phase 1 设计中**
> 前置阅读: `00-index.md`、`01-architecture.md`、`02-database.md`、`03-api-spec.md`

---

## 1. 模块定位

| 项目 | 内容 |
|------|------|
| Maven artifactId | `oa-platform-common` |
| 包名 | `cn.oa.platform.common` |
| 职责 | 通用工具/异常/常量/分页/响应包装/AOP/缓存抽象 |
| 依赖 | 无（最底层平台模块） |
| 被依赖 | 所有业务模块 + oa-platform-security + oa-workflow |

---

## 2. 目录结构

```
oa-platform-common/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/cn/oa/platform/common/
    │   │   ├── annotation/         # 自定义注解
    │   │   │   ├── Idempotent.java
    │   │   │   ├── RateLimit.java
    │   │   │   ├── DataScope.java
    │   │   │   ├── DictFormat.java
    │   │   │   └── RepeatSubmit.java
    │   │   ├── api/                 # 统一响应
    │   │   │   ├── R.java
    │   │   │   ├── PageResult.java
    │   │   │   ├── ResultCode.java
    │   │   │   └── RCode.java
    │   │   ├── constant/            # 常量
    │   │   │   ├── CommonConstants.java
    │   │   │   ├── SecurityConstants.java
    │   │   │   ├── HeaderConstants.java
    │   │   │   └── DictConstants.java
    │   │   ├── enums/               # 通用枚举
    │   │   │   ├── StatusEnum.java
    │   │   │   ├── YesNoEnum.java
    │   │   │   ├── GenderEnum.java
    │   │   │   └── BizTypeEnum.java
    │   │   ├── exception/           # 异常
    │   │   │   ├── BizException.java
    │   │   │   ├── ParamException.java
    │   │   │   ├── AuthException.java
    │   │   │   ├── ForbiddenException.java
    │   │   │   ├── NotFoundException.java
    │   │   │   └── GlobalExceptionHandler.java
    │   │   ├── util/                # 工具
    │   │   │   ├── DateUtil.java
    │   │   │   ├── JsonUtil.java
    │   │   │   ├── IdUtil.java
    │   │   │   ├── SnowflakeIdGenerator.java
    │   │   │   ├── BeanCopyUtil.java
    │   │   │   ├── ValidationUtil.java
    │   │   │   ├── CollectionUtil.java
    │   │   │   ├── PageUtil.java
    │   │   │   ├── IpUtil.java
    │   │   │   ├── UserAgentUtil.java
    │   │   │   ├── SecureUtil.java
    │   │   │   ├── HashUtil.java
    │   │   │   ├── RSAUtil.java
    │   │   │   └── ...
    │   │   ├── aop/                 # AOP
    │   │   │   ├── IdempotentAspect.java
    │   │   │   ├── RateLimitAspect.java
    │   │   │   ├── DataScopeAspect.java
    │   │   │   ├── DictFormatAspect.java
    │   │   │   ├── RepeatSubmitAspect.java
    │   │   │   └── OperationLogAspect.java
    │   │   ├── cache/               # 缓存抽象
    │   │   │   ├── CacheService.java
    │   │   │   ├── LocalCacheService.java
    │   │   │   ├── RedisCacheService.java
    │   │   │   └── CacheKeyBuilder.java
    │   │   ├── lock/                # 分布式锁
    │   │   │   ├── DistributedLock.java
    │   │   │   ├── RedisDistributedLock.java
    │   │   │   └── LockKeyBuilder.java
    │   │   ├── id/                  # ID 生成
    │   │   │   ├── IdGenerator.java
    │   │   │   ├── SnowflakeIdGen.java
    │   │   │   ├── BusinessNoGenerator.java
    │   │   │   └── IdGenType.java
    │   │   ├── tenant/              # 多租户（v2 留位）
    │   │   │   ├── TenantContext.java
    │   │   │   ├── TenantInterceptor.java
    │   │   │   └── TenantAspect.java
    │   │   ├── trace/               # 链路追踪
    │   │   │   ├── TraceContext.java
    │   │   │   ├── TraceFilter.java
    │   │   │   └── MDCUtil.java
    │   │   ├── event/               # 事件
    │   │   │   ├── BaseEvent.java
    │   │   │   ├── EventPublisher.java
    │   │   │   └── EventListener.java
    │   │   ├── base/                # 基础 Entity/BO
    │   │   │   ├── BaseEntity.java
    │   │   │   ├── BaseDTO.java
    │   │   │   ├── BaseVO.java
    │   │   │   ├── BaseBO.java
    │   │   │   ├── BaseQuery.java
    │   │   │   └── TreeEntity.java
    │   │   ├── config/              # 配置
    │   │   │   ├── CommonProperties.java
    │   │   │   ├── JacksonConfig.java
    │   │   │   ├── MybatisPlusConfig.java
    │   │   │   ├── WebConfig.java
    │   │   │   ├── AsyncConfig.java
    │   │   │   └── OpenApiConfig.java
    │   │   └── PlatformCommonAutoConfiguration.java  # Spring Boot 自动配置
    │   └── resources/
    │       ├── META-INF/
    │       │   └── spring/
    │       │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    │       └── messages/
    │           ├── messages.properties
    │           └── messages_zh_CN.properties
    └── test/
        └── java/cn/oa/platform/common/
            ├── util/
            ├── aop/
            └── ...
```

---

## 3. 核心类设计

### 3.1 R 统一响应

```java
package cn.oa.platform.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "统一响应")
public class R<T> {
    @Schema(description = "错误码, 0=成功")
    private Integer code;
    
    @Schema(description = "错误信息")
    private String message;
    
    @Schema(description = "业务数据")
    private T data;
    
    @Schema(description = "链路追踪ID")
    private String traceId;
    
    @Schema(description = "服务端时间戳(毫秒)")
    private Long timestamp;
    
    public R(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = TraceContext.getTraceId();
        this.timestamp = System.currentTimeMillis();
    }
    
    public static <T> R<T> ok() {
        return new R<>(0, "ok", null);
    }
    
    public static <T> R<T> ok(T data) {
        return new R<>(0, "ok", data);
    }
    
    public static <T> R<T> ok(String message, T data) {
        return new R<>(0, message, data);
    }
    
    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message, null);
    }
    
    public static <T> R<T> fail(ResultCode rc) {
        return new R<>(rc.getCode(), rc.getMessage(), null);
    }
    
    public static <T> R<T> fail(ResultCode rc, String message) {
        return new R<>(rc.getCode(), message, null);
    }
    
    public boolean isSuccess() {
        return this.code != null && this.code == 0;
    }
}
```

### 3.2 PageResult 分页响应

```java
package cn.oa.platform.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应")
public class PageResult<T> {
    @Schema(description = "数据列表")
    private List<T> list;
    
    @Schema(description = "总记录数")
    private Long total;
    
    @Schema(description = "页码(从 1 开始)")
    private Integer pageNum;
    
    @Schema(description = "每页大小")
    private Integer pageSize;
    
    @Schema(description = "总页数")
    private Integer pages;
    
    public static <T> PageResult<T> empty() {
        return new PageResult<>(List.of(), 0L, 1, 10, 0);
    }
    
    public static <T> PageResult<T> of(List<T> list, long total, int pageNum, int pageSize) {
        int pages = pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return new PageResult<>(list, total, pageNum, pageSize, pages);
    }
    
    /** 转换内部 VO（常用于 Entity -> VO 转换） */
    public <R> PageResult<R> map(Function<T, R> converter) {
        List<R> mappedList = this.list.stream().map(converter).collect(Collectors.toList());
        return new PageResult<>(mappedList, this.total, this.pageNum, this.pageSize, this.pages);
    }
}
```

### 3.3 ResultCode 错误码接口

```java
package cn.oa.platform.common.api;

public interface ResultCode {
    Integer getCode();
    String getMessage();
}
```

**RCode 默认实现**：

```java
package cn.oa.platform.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RCode implements ResultCode {
    SUCCESS(0, "ok"),
    
    BAD_REQUEST(1, "参数错误"),
    VALIDATION_FAILED(2, "参数校验失败"),
    NOT_FOUND(101, "资源不存在"),
    METHOD_NOT_ALLOWED(102, "方法不允许"),
    UNSUPPORTED_MEDIA_TYPE(103, "不支持的媒体类型"),
    
    UNAUTHORIZED(10001, "未登录"),
    TOKEN_EXPIRED(10002, "Token 过期"),
    INVALID_TOKEN(10003, "Token 无效"),
    SIGN_INVALID(10004, "签名错误"),
    
    FORBIDDEN(20001, "无权限"),
    DATA_PERMISSION_DENIED(20002, "数据权限不足"),
    
    RATE_LIMIT_EXCEEDED(30001, "请求过于频繁"),
    IDEMPOTENT_CONFLICT(30002, "幂等冲突"),
    
    INTERNAL_ERROR(99001, "服务内部错误"),
    SERVICE_UNAVAILABLE(99002, "服务暂不可用"),
    DB_ERROR(99003, "数据库错误"),
    THIRD_PARTY_ERROR(99004, "第三方服务错误"),
    UNKNOWN(99999, "未知错误");
    
    private final Integer code;
    private final String message;
}
```

### 3.4 BizException 业务异常

```java
package cn.oa.platform.common.exception;

import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.api.ResultCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final Integer code;
    
    public BizException(String message) {
        super(message);
        this.code = RCode.INTERNAL_ERROR.getCode();
    }
    
    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }
    
    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
    
    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
```

**子类**（语义化）：
```java
public class ParamException extends BizException {
    public ParamException(String message) {
        super(RCode.BAD_REQUEST, message);
    }
}

public class AuthException extends BizException {
    public AuthException() { super(RCode.UNAUTHORIZED); }
    public AuthException(String message) { super(RCode.UNAUTHORIZED, message); }
}

public class ForbiddenException extends BizException {
    public ForbiddenException() { super(RCode.FORBIDDEN); }
    public ForbiddenException(String message) { super(RCode.FORBIDDEN, message); }
}

public class NotFoundException extends BizException {
    public NotFoundException(String resource) {
        super(RCode.NOT_FOUND, resource + " 不存在");
    }
}
```

### 3.5 GlobalExceptionHandler 全局异常处理

```java
package cn.oa.platform.common.exception;

import cn.oa.platform.common.api.R;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.api.ResultCode;
import cn.oa.platform.common.trace.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BizException.class)
    public R<?> handleBiz(BizException e, HttpServletRequest request) {
        log.warn("BizException: code={}, message={}, uri={}", e.getCode(), e.getMessage(), request.getRequestURI());
        return R.fail(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public R<?> handleValidation(MethodArgumentNotValidException e) {
        List<FieldError> errors = e.getBindingResult().getFieldErrors();
        List<ValidationError> validationErrors = new ArrayList<>();
        for (FieldError error : errors) {
            validationErrors.add(new ValidationError(error.getField(), error.getDefaultMessage()));
        }
        return R.fail(RCode.VALIDATION_FAILED.getCode(), RCode.VALIDATION_FAILED.getMessage());
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public R<?> handleConstraintViolation(ConstraintViolationException e) {
        List<ValidationError> errors = new ArrayList<>();
        for (ConstraintViolation<?> v : e.getConstraintViolations()) {
            String path = v.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            errors.add(new ValidationError(field, v.getMessage()));
        }
        return R.fail(RCode.VALIDATION_FAILED.getCode(), RCode.VALIDATION_FAILED.getMessage());
    }
    
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<?> handleNotFound(Exception e) {
        return R.fail(RCode.NOT_FOUND);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<?> handleBadRequest(HttpMessageNotReadableException e) {
        return R.fail(RCode.BAD_REQUEST, "请求体格式错误");
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<?> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(RCode.BAD_REQUEST, "缺少参数: " + e.getParameterName());
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<?> handleUnknown(Exception e, HttpServletRequest request) {
        log.error("Unknown exception: uri={}", request.getRequestURI(), e);
        return R.fail(RCode.INTERNAL_ERROR);
    }
}
```

### 3.6 BaseEntity

```java
package cn.oa.platform.common.base;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "基础实体")
public abstract class BaseEntity implements Serializable {
    
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;
    
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人")
    private String createBy;
    
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人")
    private String updateBy;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField(select = false)
    @JsonIgnore
    @Schema(description = "软删除标志, 0=正常 1=删除")
    private String delFlag;
    
    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;
}
```

### 3.7 MybatisPlusConfig

```java
package cn.oa.platform.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import cn.oa.platform.common.base.BaseEntity;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.common.util.MetaObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@Configuration
@EnableTransactionManagement
public class MybatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(1000L);
        pagination.setOverflow(true);
        interceptor.addInnerInterceptor(pagination);
        
        // 乐观锁
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        // 防全表更新与删除
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        
        return interceptor;
    }
    
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                if (metaObject.hasSetter("createBy")) {
                    this.strictInsertFill(metaObject, "createBy", String.class, getCurrentUser());
                }
                if (metaObject.hasSetter("createTime")) {
                    this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                }
                if (metaObject.hasSetter("updateBy")) {
                    this.strictInsertFill(metaObject, "updateBy", String.class, getCurrentUser());
                }
                if (metaObject.hasSetter("updateTime")) {
                    this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                }
                if (metaObject.hasSetter("delFlag")) {
                    this.strictInsertFill(metaObject, "delFlag", String.class, "0");
                }
            }
            
            @Override
            public void updateFill(MetaObject metaObject) {
                if (metaObject.hasSetter("updateBy")) {
                    this.strictUpdateFill(metaObject, "updateBy", String.class, getCurrentUser());
                }
                if (metaObject.hasSetter("updateTime")) {
                    this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                }
            }
            
            private String getCurrentUser() {
                return UserContext.getCurrentEmpId() == null ? "system" : String.valueOf(UserContext.getCurrentEmpId());
            }
        };
    }
}
```

### 3.8 UserContext 当前用户上下文

```java
package cn.oa.platform.common.context;

/**
 * 当前用户 ThreadLocal 上下文
 * 由 oa-platform-security 的拦截器设置
 */
public class UserContext {
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();
    
    public static void set(UserInfo userInfo) {
        THREAD_LOCAL.set(userInfo);
    }
    
    public static UserInfo get() {
        return THREAD_LOCAL.get();
    }
    
    public static Long getCurrentEmpId() {
        UserInfo info = get();
        return info == null ? null : info.getEmpId();
    }
    
    public static String getCurrentUsername() {
        UserInfo info = get();
        return info == null ? null : info.getUsername();
    }
    
    public static void clear() {
        THREAD_LOCAL.remove();
    }
    
    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private Long empId;
        private String username;
        private String realName;
        private Long deptId;
        private String deptName;
        private String dataScope;
        private List<String> roles;
        private List<String> permissions;
    }
}
```

### 3.9 Idempotent 幂等注解 + 切面

```java
package cn.oa.platform.common.annotation;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    /** Header key, 默认 "Idempotency-Key" */
    String headerKey() default "Idempotency-Key";
    /** SpEL 表达式, 从参数提取 */
    String key() default "";
    /** 过期时间(秒) */
    long ttl() default 86400L;
    /** 是否校验同 key 同 body */
    boolean checkBody() default true;
}
```

```java
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        String key = resolveKey(pjp, idempotent);
        if (key == null || key.isEmpty()) {
            return pjp.proceed();
        }
        
        // 1. 检查缓存
        String cacheKey = "idempotent:" + key;
        String cached = cacheService.get(cacheKey);
        if (cached != null) {
            log.info("Idempotent hit: {}", key);
            return objectMapper.readValue(cached, R.class);
        }
        
        // 2. 执行
        Object result = pjp.proceed();
        
        // 3. 缓存结果
        cacheService.set(cacheKey, objectMapper.writeValueAsString(result), idempotent.ttl());
        
        return result;
    }
    
    private String resolveKey(ProceedingJoinPoint pjp, Idempotent idempotent) {
        // 优先使用 header，其次 SpEL
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String headerKey = request.getHeader(idempotent.headerKey());
        if (headerKey != null && !headerKey.isEmpty()) {
            return headerKey;
        }
        if (!idempotent.key().isEmpty()) {
            return SpELUtil.eval(idempotent.key(), pjp);
        }
        return null;
    }
}
```

### 3.10 RateLimit 限流注解 + 切面

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String key() default "";                    // SpEL
    RateLimitType type() default RateLimitType.GLOBAL;  // IP/USER/GLOBAL/INTERFACE
    int capacity() default 100;                  // 桶容量
    int refill() default 10;                     // 每秒补充
    int duration() default 60;                   // 窗口(秒)
    RateLimitStrategy strategy() default RateLimitStrategy.TOKEN_BUCKET;
}
```

```java
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = buildKey(pjp, rateLimit);
        
        // 令牌桶算法
        String luaScript = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill = tonumber(ARGV[2])
            local current = tonumber(redis.call('get', key) or capacity)
            if current > 0 then
                redis.call('decr', key)
                return 1
            else
                return 0
            end
        """;
        
        Long result = redisTemplate.execute(RedisScript.of(luaScript, Long.class),
            List.of(key),
            String.valueOf(rateLimit.capacity()),
            String.valueOf(rateLimit.refill()));
        
        if (result == 0) {
            throw new BizException(RCode.RATE_LIMIT_EXCEEDED);
        }
        
        return pjp.proceed();
    }
}
```

### 3.11 DataScope 数据权限注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    /** 数据范围 */
    DataScopeType value() default DataScopeType.DEPT;
    /** 部门字段名(用于 WHERE 拼接) */
    String deptColumn() default "dept_id";
    /** 个人字段名 */
    String userColumn() default "create_by";
}

public enum DataScopeType {
    SELF, DEPT, DEPT_DOWN, COMPANY, ALL
}
```

```java
@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {
    
    @Before("@annotation(dataScope)")
    public void doBefore(JoinPoint point, DataScope dataScope) {
        // 1. 获取当前用户数据权限
        UserContext.UserInfo user = UserContext.get();
        if (user == null) return;
        
        // 2. 解析注解
        String userColumn = dataScope.userColumn();
        String deptColumn = dataScope.deptColumn();
        DataScopeType scope = dataScope.value();
        
        // 3. 拼接 SQL 条件
        String sqlCondition = buildSqlCondition(user, scope, deptColumn, userColumn);
        
        // 4. 放入 ThreadLocal
        DataScopeContextHolder.set(sqlCondition);
    }
    
    @AfterReturning("@annotation(dataScope)")
    public void doAfter() {
        DataScopeContextHolder.clear();
    }
}
```

**MyBatis-Plus 拦截器拼接条件**：

```java
public class DataScopeInnerInterceptor implements InnerInterceptor {
    
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        String condition = DataScopeContextHolder.get();
        if (condition == null || condition.isEmpty()) {
            return;
        }
        
        // 改写 SQL 拼接条件
        String originalSql = boundSql.getSql();
        String newSql = originalSql + " AND (" + condition + ")";
        // 通过反射设置 BoundSql.sql
        ReflectUtil.setFieldValue(boundSql, "sql", newSql);
    }
}
```

### 3.12 TraceContext 链路追踪

```java
public class TraceContext {
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();
    
    public static String getTraceId() {
        return TRACE_ID.get();
    }
    
    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }
    
    public static Long getStartTime() {
        return START_TIME.get();
    }
    
    public static void setStartTime(Long time) {
        START_TIME.set(time);
    }
    
    public static void clear() {
        TRACE_ID.remove();
        START_TIME.remove();
        MDC.clear();
    }
}
```

```java
public class TraceFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        
        // 1. 获取或生成 traceId
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = IdUtil.fastSimpleUUID();
        }
        TraceContext.setTraceId(traceId);
        TraceContext.setStartTime(System.currentTimeMillis());
        MDC.put("traceId", traceId);
        
        // 2. 响应头
        response.setHeader("X-Trace-Id", traceId);
        
        try {
            chain.doFilter(req, res);
        } finally {
            TraceContext.clear();
        }
    }
}
```

### 3.13 SnowflakeIdGenerator 雪花 ID

```java
public class SnowflakeIdGenerator {
    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }
    
    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 4095;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - 1288834974657L) << 22)
                | (datacenterId << 17)
                | (workerId << 12)
                | sequence;
    }
    
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }
    
    private long timeGen() {
        return System.currentTimeMillis();
    }
}
```

**业务单号生成器**：

```java
@Component
public class BusinessNoGenerator {
    
    @Autowired
    private SnowflakeIdGenerator idGenerator;
    
    public String generate(String prefix) {
        long id = idGenerator.nextId();
        return prefix + DateUtil.format(LocalDate.now(), "yyyyMMdd") + String.format("%010d", id % 10000000000L);
    }
    
    public String generateLeaveNo() { return generate("LV"); }
    public String generateExpenseNo() { return generate("EX"); }
    public String generateLoanNo() { return generate("LN"); }
    public String generateDispatchNo() { return generate("DC"); }
}
```

---

## 4. 配置文件

### 4.1 application-common.yml

```yaml
spring:
  application:
    name: oa-system
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false

oa:
  common:
    id-generator:
      worker-id: 1
      datacenter-id: 1
    cache:
      type: redis  # local / redis
      default-ttl: 600
    business-no:
      prefix: "OA"
```

---

## 5. 测试

### 5.1 单元测试样例

```java
class IdempotentAspectTest {
    
    @Test
    void shouldCacheResult() {
        // ...
    }
}
```

---

## 6. 后续阶段

- Phase 2: 实现本模块所有类
- Phase 3: 单元测试覆盖率 > 80%
- Phase 4: 集成到 oa-platform-web 启动模块
