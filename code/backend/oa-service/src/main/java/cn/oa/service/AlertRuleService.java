package cn.oa.service;

import cn.oa.entity.RptAlertRule;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AlertRuleService extends IService<RptAlertRule> {

    IPage<RptAlertRule> pageList(int pageNum, int pageSize, String ruleType);
}