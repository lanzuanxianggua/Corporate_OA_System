package cn.oa.workflow.core.resolver;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 固定人员解析器 - 直接返回指定员工ID
 * ruleValue: 用户ID或用户ID数组JSON
 *            如 "123" 或 "[123, 456, 789]"
 */
@Slf4j
@Component
@Order(10)
public class FixedUserResolver implements AssigneeResolver {

    @Override
    public String getRuleType() {
        return "FIXED_USER";
    }

    @Override
    public List<Long> resolve(String ruleValue, Long starterId, String formDataSnapshot) {
        try {
            Long userId = Long.valueOf(ruleValue);
            log.debug("FixedUserResolver: resolved userId={}", userId);
            return Collections.singletonList(userId);
        } catch (NumberFormatException e) {
            // ruleValue might be a JSON array of user IDs
            try {
                return JSONUtil.parseArray(ruleValue).toList(Long.class);
            } catch (Exception ex) {
                log.error("FixedUserResolver: failed to parse ruleValue={}", ruleValue, ex);
                return Collections.emptyList();
            }
        }
    }
}