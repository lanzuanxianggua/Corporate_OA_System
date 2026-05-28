package cn.oa.common.annotation;

import java.lang.annotation.*;

/**
 * Marks a controller method or class as requiring one of the specified roles.
 * The user must have at least ONE of the listed roles to access the endpoint.
 * ADMIN role always bypasses this check.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * Role keys that are allowed to access this endpoint.
     * The user needs at least one matching role.
     * ADMIN always passes regardless of this list.
     */
    String[] value() default {};
}
