package cn.oa.common.exception;

import lombok.Getter;

/**
 * 认证/授权异常
 */
@Getter
public class AuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 错误码 */
    private final Integer code;

    public AuthException(String message) {
        super(message);
        this.code = 401;
    }

    public AuthException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
