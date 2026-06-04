package cn.oa.platform.common.exception;

import cn.oa.platform.common.api.RCode;

public class ParamException extends BizException {
    public ParamException(String message) {
        super(RCode.BAD_REQUEST, message);
    }
}
