package cn.oa.platform.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等注解 (占位 - 详细 AOP 实现见 Phase 3 集成).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    String headerKey() default "Idempotency-Key";

    String key() default "";

    long ttl() default 86400L;

    boolean checkBody() default true;
}
