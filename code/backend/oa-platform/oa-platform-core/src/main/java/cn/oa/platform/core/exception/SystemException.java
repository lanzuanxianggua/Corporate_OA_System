package cn.oa.platform.core.exception;

import lombok.Getter;

/**
 * 系统异常
 * 用于系统级别不可预期的异常
 *
 * @author oa-platform
 */
@Getter
public class SystemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    public SystemException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    public SystemException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public SystemException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
