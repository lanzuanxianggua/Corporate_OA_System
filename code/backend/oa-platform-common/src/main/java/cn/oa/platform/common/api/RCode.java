package cn.oa.platform.common.api;

import java.util.Objects;

/**
 * 默认错误码实现 (v2).
 */
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

    RCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public static RCode of(Integer code) {
        for (RCode rc : values()) {
            if (Objects.equals(rc.code, code)) {
                return rc;
            }
        }
        return UNKNOWN;
    }
}
