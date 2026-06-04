package cn.oa.workflow.engine;

import cn.oa.workflow.entity.WfAssigneeRule;
import cn.oa.workflow.mapper.WfAssigneeRuleMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 审批人解析器.
 *
 * <p>v2 简化为 4 种规则:
 * <ul>
 *   <li>ROLE - 角色: 解析为拥有该角色的员工 (默认 empId=1, 真实系统查 sys_emp_role)</li>
 *   <li>LEADER - 上级: 取发起人的 leader_id (默认 empId=2, 真实系统查 sys_emp.leader_id)</li>
 *   <li>SELF - 发起人</li>
 *   <li>FIXED - 固定 empId (rule_target 解析为 long)</li>
 * </ul>
 *
 * <p>在 v2 Phase 2 阶段, ROLE 和 LEADER 走默认实现 (返回测试 empId),
 * 业务方可在 sys 模块接入真实查询.
 */
@Component
public class WfAssigneeResolver {

    private final WfAssigneeRuleMapper ruleMapper;

    public WfAssigneeResolver(WfAssigneeRuleMapper ruleMapper) {
        this.ruleMapper = ruleMapper;
    }

    /**
     * 解析节点审批人.
     *
     * @param defId    流程定义 ID
     * @param nodeKey  节点 KEY
     * @param initiator 发起人 empId
     * @return 审批人 empId, 找不到规则时返回发起人
     */
    public Long resolve(Long defId, String nodeKey, Long initiator) {
        List<WfAssigneeRule> rules = ruleMapper.findByDefId(defId);
        // v2 简化: 返回第一个匹配节点的规则; 真实实现需按 node_id 关联
        if (rules.isEmpty()) {
            return initiator;
        }
        WfAssigneeRule rule = rules.get(0);
        return switch (rule.getRuleType()) {
            case "ROLE"   -> 1L; // 默认 HR 角色 emp_id=1 (测试)
            case "LEADER" -> 2L; // 默认 manager emp_id=2 (测试)
            case "SELF"   -> initiator;
            case "FIXED"  -> Long.parseLong(rule.getRuleTarget());
            default       -> initiator;
        };
    }
}
