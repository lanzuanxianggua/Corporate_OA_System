package cn.oa.service;

import cn.oa.common.result.PageResult;
import cn.oa.entity.OaOperationLog;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OperationLogService extends IService<OaOperationLog> {
    PageResult<OaOperationLog> pageList(int pageNum, int pageSize, String module, String startTime, String endTime);
}
