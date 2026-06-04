package cn.oa.platform.common.exception;

import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.api.ResultCode;

/**
 * 业务异常基类.
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = RCode.INTERNAL_ERROR.getCode();
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
