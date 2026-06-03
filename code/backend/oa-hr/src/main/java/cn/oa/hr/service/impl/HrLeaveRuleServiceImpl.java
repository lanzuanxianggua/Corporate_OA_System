package cn.oa.hr.service.impl;

import cn.oa.hr.entity.HrLeaveRule;
import cn.oa.hr.enums.HrLeaveType;
import cn.oa.hr.mapper.HrLeaveRuleMapper;
import cn.oa.hr.service.HrLeaveRuleService;
import cn.oa.hr.vo.HrLeaveRuleVO;
import cn.oa.platform.core.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HR假期规则服务实现
 *
 * @author oa-hr
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrLeaveRuleServiceImpl implements HrLeaveRuleService {

    private final HrLeaveRuleMapper ruleMapper;

    @Override
    public List<HrLeaveRuleVO> listActiveRules() {
        LambdaQueryWrapper<HrLeaveRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrLeaveRule::getStatus, "ACTIVE")
                .orderByAsc(HrLeaveRule::getLeaveType);

        List<HrLeaveRule> rules = ruleMapper.selectList(wrapper);
        return rules.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public HrLeaveRule getRuleByLeaveType(String leaveType) {
        return ruleMapper.selectOne(new LambdaQueryWrapper<HrLeaveRule>()
                .eq(HrLeaveRule::getLeaveType, leaveType)
                .eq(HrLeaveRule::getStatus, "ACTIVE"));
    }

    @Override
    @Transactional
    public void updateRule(HrLeaveRule rule) {
        HrLeaveRule existing = ruleMapper.selectById(rule.getId());
        if (existing == null) {
            throw new BusinessException("假期规则不存在");
        }

        // 只允许更新部分字段
        existing.setRuleName(rule.getRuleName());
        existing.setMinUnit(rule.getMinUnit());
        existing.setMaxDaysPerApply(rule.getMaxDaysPerApply());
        existing.setDeductBalance(rule.getDeductBalance());
        existing.setDeductSalary(rule.getDeductSalary());
        existing.setRequireAttachment(rule.getRequireAttachment());
        existing.setRuleScript(rule.getRuleScript());

        ruleMapper.updateById(existing);
        log.info("Updated leave rule: id={}, type={}", rule.getId(), existing.getLeaveType());
    }

    @Override
    public String validateLeaveRequest(String leaveType, BigDecimal days, boolean hasAttachment) {
        // 检查假期类型是否有效
        HrLeaveType type = HrLeaveType.fromCode(leaveType);
        if (type == null) {
            return "无效的假期类型: " + leaveType;
        }

        // 获取规则
        HrLeaveRule rule = getRuleByLeaveType(leaveType);
        if (rule == null) {
            // 没有规则，使用默认校验
            return null;
        }

        // 校验最小单位
        BigDecimal minUnit = rule.getMinUnit();
        if (minUnit != null && minUnit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remainder = days.remainder(minUnit);
            if (remainder.compareTo(BigDecimal.ZERO) != 0) {
                return "请假天数必须是 " + minUnit + " 天的整数倍";
            }
        }

        // 校验单次最大天数
        BigDecimal maxDays = rule.getMaxDaysPerApply();
        if (maxDays != null && maxDays.compareTo(BigDecimal.ZERO) > 0) {
            if (days.compareTo(maxDays) > 0) {
                return "单次请假天数不能超过 " + maxDays + " 天";
            }
        }

        // 校验附件要求
        if (Integer.valueOf(1).equals(rule.getRequireAttachment()) && !hasAttachment) {
            return type.getName() + "需要上传附件";
        }

        return null;
    }

    /**
     * 检查假期类型是否需要扣减余额
     *
     * @param leaveType 假期类型
     * @return 是否需要扣减余额
     */
    public boolean isDeductBalance(String leaveType) {
        HrLeaveRule rule = getRuleByLeaveType(leaveType);
        if (rule == null) {
            // 默认不扣减
            return false;
        }
        return Integer.valueOf(1).equals(rule.getDeductBalance());
    }

    private HrLeaveRuleVO toVO(HrLeaveRule rule) {
        HrLeaveRuleVO vo = new HrLeaveRuleVO();
        vo.setId(rule.getId());
        vo.setRuleName(rule.getRuleName());
        vo.setLeaveType(rule.getLeaveType());
        vo.setLeaveTypeName(getLeaveTypeName(rule.getLeaveType()));
        vo.setMinUnit(rule.getMinUnit());
        vo.setMaxDaysPerApply(rule.getMaxDaysPerApply());
        vo.setDeductBalance(rule.getDeductBalance());
        vo.setDeductSalary(rule.getDeductSalary());
        vo.setRequireAttachment(rule.getRequireAttachment());
        vo.setRuleScript(rule.getRuleScript());
        vo.setStatus(rule.getStatus());
        vo.setUpdateTime(rule.getUpdateTime());
        return vo;
    }

    private String getLeaveTypeName(String leaveType) {
        HrLeaveType type = HrLeaveType.fromCode(leaveType);
        return type != null ? type.getName() : leaveType;
    }
}
