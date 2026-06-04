package cn.oa.platform.common.trace;

import org.slf4j.MDC;

/**
 * 链路追踪上下文.
 */
public final class TraceContext {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String MDC_TRACE_ID = "traceId";

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    private TraceContext() {}

    public static String getTraceId() {
        return TRACE_ID.get();
    }

    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
        if (traceId != null) {
            MDC.put(MDC_TRACE_ID, traceId);
        }
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
        MDC.remove(MDC_TRACE_ID);
    }
}
