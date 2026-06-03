package cn.oa.workflow.core.resolver;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 表单选择解析器 - 从表单数据中解析用户选择的审批人
 * ruleValue: 表单字段名，如 "approver_id" 或 "approvers"
 * formDataSnapshot 中应包含该字段的值
 */
@Slf4j
@Component
@Order(50)
public class FormSelectResolver implements AssigneeResolver {

    @Override
    public String getRuleType() {
        return "FORM_SELECT";
    }

    @Override
    public List<Long> resolve(String ruleValue, Long starterId, String formDataSnapshot) {
        if (ruleValue == null || ruleValue.isBlank()) {
            log.warn("FormSelectResolver: ruleValue is empty");
            return Collections.emptyList();
        }

        if (formDataSnapshot == null || formDataSnapshot.isBlank()) {
            log.warn("FormSelectResolver: formDataSnapshot is empty");
            return Collections.emptyList();
        }

        log.debug("FormSelectResolver: resolving from form field={}", ruleValue);

        try {
            var formData = JSONUtil.parseObj(formDataSnapshot);

            // 检查字段是否存在
            if (!formData.containsKey(ruleValue)) {
                log.warn("FormSelectResolver: field {} not found in form data", ruleValue);
                return Collections.emptyList();
            }

            Object value = formData.get(ruleValue);
            if (value == null) {
                return Collections.emptyList();
            }

            // 处理单个用户ID
            if (value instanceof Number) {
                return Collections.singletonList(((Number) value).longValue());
            }

            // 处理字符串形式的用户ID
            if (value instanceof String) {
                String strValue = (String) value;
                try {
                    return Collections.singletonList(Long.parseLong(strValue));
                } catch (NumberFormatException e) {
                    // 可能是JSON数组字符串
                    if (strValue.startsWith("[")) {
                        return JSONUtil.parseArray(strValue).toList(Long.class);
                    }
                    log.warn("FormSelectResolver: invalid user ID string: {}", strValue);
                    return Collections.emptyList();
                }
            }

            // 处理数组
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<?> list = (List<?>) value;
                return list.stream()
                        .map(item -> {
                            if (item instanceof Number) {
                                return ((Number) item).longValue();
                            }
                            if (item instanceof String) {
                                try {
                                    return Long.parseLong((String) item);
                                } catch (NumberFormatException e) {
                                    return null;
                                }
                            }
                            return null;
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList());
            }

            log.warn("FormSelectResolver: unsupported value type: {}", value.getClass());
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("FormSelectResolver: error parsing form data", e);
            return Collections.emptyList();
        }
    }
}
