package cn.oa.common.exception;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 错误码 */
    private final int code;

    public BusinessException(String msg) {
        super(msg);
        this.code = 500;
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
