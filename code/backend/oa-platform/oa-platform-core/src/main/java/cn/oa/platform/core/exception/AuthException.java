package cn.oa.platform.core.exception;

import lombok.Getter;

/**
 * 认证异常
 * 用于身份认证失败时抛出的异常
 *
 * @author oa-platform
 */
@Getter
public class AuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    public AuthException(String message) {
        super(message);
        this.code = 401;
        this.message = message;
    }

    public AuthException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public AuthException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
