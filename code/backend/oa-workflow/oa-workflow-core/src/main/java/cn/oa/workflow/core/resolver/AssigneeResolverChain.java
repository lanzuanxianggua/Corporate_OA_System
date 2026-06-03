package cn.oa.workflow.core.resolver;

import cn.oa.workflow.model.entity.WfAssigneeRule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 审批人解析器责任链
 * 按优先级依次调用各解析器，直到解析出审批人
 */
@Slf4j
@Component
public class AssigneeResolverChain {

    @Autowired
    private List<AssigneeResolver> resolvers;

    /**
     * 解析器映射表（按规则类型）
     */
    private Map<String, AssigneeResolver> resolverMap;

    @PostConstruct
    public void init() {
        resolverMap = new LinkedHashMap<>();
        if (resolvers != null) {
            // 按 @Order 注解排序
            resolvers.stream()
                    .sorted(Comparator.comparingInt(this::getOrder))
                    .forEach(r -> {
                        resolverMap.put(r.getRuleType(), r);
                        log.info("Registered AssigneeResolver: {} (order={})",
                                r.getRuleType(), getOrder(r));
                    });
        }
    }

    private int getOrder(AssigneeResolver resolver) {
        Order order = resolver.getClass().getAnnotation(Order.class);
        return order != null ? order.value() : Integer.MAX_VALUE;
    }

    /**
     * 根据规则解析审批人
     *
     * @param rule            审批人规则
     * @param starterId       发起人ID
     * @param formDataSnapshot 表单数据快照(JSON)
     * @return 审批人ID列表
     */
    public List<Long> resolve(WfAssigneeRule rule, Long starterId, String formDataSnapshot) {
        AssigneeResolver resolver = resolverMap.get(rule.getRuleType());
        if (resolver == null) {
            log.warn("No resolver found for rule type: {}", rule.getRuleType());
            return Collections.emptyList();
        }

        try {
            List<Long> result = resolver.resolve(rule.getRuleValue(), starterId, formDataSnapshot);
            log.debug("Resolver {} returned {} assignees for ruleValue={}",
                    rule.getRuleType(), result.size(), rule.getRuleValue());
            return result;
        } catch (Exception e) {
            log.error("Resolver {} failed for ruleValue={}",
                    rule.getRuleType(), rule.getRuleValue(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 批量解析多规则的审批人（合并去重）
     *
     * @param rules           规则列表
     * @param starterId       发起人ID
     * @param formDataSnapshot 表单数据快照
     * @return 合并后的审批人ID列表
     */
    public List<Long> resolveAll(List<WfAssigneeRule> rules, Long starterId, String formDataSnapshot) {
        Set<Long> assigneeSet = new LinkedHashSet<>();

        for (WfAssigneeRule rule : rules) {
            List<Long> resolved = resolve(rule, starterId, formDataSnapshot);
            assigneeSet.addAll(resolved);
        }

        return new ArrayList<>(assigneeSet);
    }

    /**
     * 获取指定类型的解析器
     *
     * @param ruleType 规则类型
     * @return 解析器实例
     */
    public AssigneeResolver getResolver(String ruleType) {
        return resolverMap.get(ruleType);
    }

    /**
     * 检查是否存在指定类型的解析器
     *
     * @param ruleType 规则类型
     * @return 是否存在
     */
    public boolean hasResolver(String ruleType) {
        return resolverMap.containsKey(ruleType);
    }
}
