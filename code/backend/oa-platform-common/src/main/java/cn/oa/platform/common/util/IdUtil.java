package cn.oa.platform.common.util;

import cn.oa.platform.common.trace.TraceContext;

import java.util.UUID;

/**
 * ID 工具.
 */
public final class IdUtil {

    private IdUtil() {}

    /** 短 UUID (32 字符) */
    public static String fastSimpleUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 标准 UUID (36 字符) */
    public static String simpleUUID() {
        return UUID.randomUUID().toString();
    }

    /** 生成基于 traceId 的唯一 ID */
    public static String traceId() {
        String tid = TraceContext.getTraceId();
        return tid != null ? tid : fastSimpleUUID();
    }
}
