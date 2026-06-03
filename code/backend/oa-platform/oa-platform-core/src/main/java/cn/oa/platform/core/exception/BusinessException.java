package cn.oa.platform.core.exception;

import lombok.Getter;

/**
 * 业务异常
 * 用于业务逻辑中抛出的可预期异常
 *
 * @author oa-platform
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    public BusinessException(String message) {
        super(message);
        this.code = -1;
        this.message = message;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
