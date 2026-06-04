package cn.oa.platform.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限码注解.
 *
 * <p>用于 Controller 方法或类上. 运行时由 PermissionInterceptor 校验.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /** 权限码, 多个用 AND 关系 (用户必须同时拥有) */
    String[] value();

    /** 校验模式: 默认 AND */
    Logical logical() default Logical.AND;

    enum Logical { AND, OR }
}
