package cn.oa.platform.security.annotation;

import java.lang.annotation.*;

/**
 * 权限注解
 * 标注在方法或类上，表示需要特定权限才能访问
 *
 * @author oa-platform
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 需要的权限列表（满足其一即可）
     */
    String[] value() default {};

    /**
     * 是否需要满足所有权限（AND 关系）
     * 默认 false，即满足其一即可（OR 关系）
     */
    boolean requireAll() default false;

    /**
     * 提示信息
     */
    String message() default "权限不足";
}