package cn.oa.workflow.core.resolver;

import java.util.List;

/**
 * 审批人解析器接口 - 策略模式
 */
public interface AssigneeResolver {

    /**
     * 解析规则类型
     */
    String getRuleType();

    /**
     * 解析审批人
     * @param ruleValue 规则值
     * @param starterId 发起人ID
     * @param formDataSnapshot 表单数据快照(JSON)
     * @return 审批人ID列表
     */
    List<Long> resolve(String ruleValue, Long starterId, String formDataSnapshot);
}