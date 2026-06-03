package cn.oa.hr.service;

import cn.oa.hr.entity.HrLeaveRule;
import cn.oa.hr.vo.HrLeaveRuleVO;

import java.util.List;

/**
 * HR假期规则服务接口
 *
 * @author oa-hr
 */
public interface HrLeaveRuleService {

    /**
     * 查询所有活跃的假期规则
     *
     * @return 规则列表
     */
    List<HrLeaveRuleVO> listActiveRules();

    /**
     * 根据假期类型查询规则
     *
     * @param leaveType 假期类型
     * @return 规则实体
     */
    HrLeaveRule getRuleByLeaveType(String leaveType);

    /**
     * 更新假期规则
     *
     * @param rule 规则实体
     */
    void updateRule(HrLeaveRule rule);

    /**
     * 校验请假是否符合规则
     *
     * @param leaveType  假期类型
     * @param days       请假天数
     * @param hasAttachment 是否有附件
     * @return 校验结果消息，null表示通过
     */
    String validateLeaveRequest(String leaveType, java.math.BigDecimal days, boolean hasAttachment);
}
