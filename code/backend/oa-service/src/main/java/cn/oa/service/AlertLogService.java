package cn.oa.service;

import cn.oa.entity.RptAlertLog;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AlertLogService extends IService<RptAlertLog> {

    IPage<RptAlertLog> pageList(int pageNum, int pageSize, Long ruleId, Character handleStatus);

    void handle(Long logId, String handler, String handleRemark);
}