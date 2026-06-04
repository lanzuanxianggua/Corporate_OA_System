package cn.oa.platform.common.exception;

import cn.oa.platform.common.api.RCode;

public class ForbiddenException extends BizException {
    public ForbiddenException() {
        super(RCode.FORBIDDEN);
    }

    public ForbiddenException(String message) {
        super(RCode.FORBIDDEN, message);
    }
}
