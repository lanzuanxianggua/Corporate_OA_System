package cn.oa.platform.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限范围注解.
 *
 * <p>用于 Service/Mapper 方法上, 由 DataScopeInterceptor 拦截.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    Scope value() default Scope.DEPT;

    /** 部门字段名 (Mapper XML 中用 ${deptColumn}) */
    String deptColumn() default "dept_id";

    /** 用户字段名 */
    String userColumn() default "create_by";

    enum Scope {
        /** 全部数据 */
        ALL,
        /** 本部门 */
        DEPT,
        /** 本部门及下级 */
        DEPT_AND_CHILD,
        /** 仅本人 */
        SELF,
        /** 自定义 */
        CUSTOM
    }
}
