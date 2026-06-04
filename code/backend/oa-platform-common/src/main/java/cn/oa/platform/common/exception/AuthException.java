package cn.oa.platform.common.exception;

import cn.oa.platform.common.api.RCode;

public class AuthException extends BizException {
    public AuthException() {
        super(RCode.UNAUTHORIZED);
    }

    public AuthException(String message) {
        super(RCode.UNAUTHORIZED, message);
    }
}
