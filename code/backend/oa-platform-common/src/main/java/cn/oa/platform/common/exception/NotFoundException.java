package cn.oa.platform.common.exception;

import cn.oa.platform.common.api.RCode;

public class NotFoundException extends BizException {
    public NotFoundException(String resource) {
        super(RCode.NOT_FOUND, resource + " 不存在");
    }
}
