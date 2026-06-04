package cn.oa.platform.common.constant;

/**
 * 平台层通用常量.
 */
public final class CommonConstants {

    private CommonConstants() {}

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    public static final String IDEMPOTENT_KEY_HEADER = "Idempotency-Key";

    public static final String DEL_FLAG_NORMAL = "0";
    public static final String DEL_FLAG_DELETED = "1";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    public static final String DEFAULT_USER = "system";
}
