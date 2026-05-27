package cn.oa.service.impl;

import cn.oa.entity.RptAlertRule;
import cn.oa.mapper.RptAlertRuleMapper;
import cn.oa.service.AlertRuleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AlertRuleServiceImpl extends ServiceImpl<RptAlertRuleMapper, RptAlertRule> implements AlertRuleService {

    @Override
    public IPage<RptAlertRule> pageList(int pageNum, int pageSize, String ruleType) {
        Page<RptAlertRule> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RptAlertRule> wrapper = new LambdaQueryWrapper<>();
        if (ruleType != null && !ruleType.isEmpty()) {
            wrapper.eq(RptAlertRule::getRuleType, ruleType);
        }
        wrapper.orderByDesc(RptAlertRule::getCreateTime);
        return this.page(page, wrapper);
    }
}