package cn.oa.workflow.core.resolver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 岗位解析器 - 解析指定岗位的所有员工
 * ruleValue: 岗位编码，如 "MANAGER", "DIRECTOR"
 */
@Slf4j
@Component
@Order(20)
public class PostResolver implements AssigneeResolver {

    // TODO: 注入 EmployeeService 或 PostMapper
    // @Autowired
    // private EmployeeService employeeService;

    @Override
    public String getRuleType() {
        return "POST";
    }

    @Override
    public List<Long> resolve(String ruleValue, Long starterId, String formDataSnapshot) {
        if (ruleValue == null || ruleValue.isBlank()) {
            log.warn("PostResolver: ruleValue is empty");
            return Collections.emptyList();
        }

        log.debug("PostResolver: resolving employees for post={}", ruleValue);

        // TODO: 调用 employeeService.findByPostCode(ruleValue)
        // 示例实现：返回空列表，等待实际服务注入
        // List<Employee> employees = employeeService.findByPostCode(ruleValue);
        // return employees.stream().map(Employee::getId).collect(Collectors.toList());

        log.warn("PostResolver: Not implemented yet for post={}", ruleValue);
        return Collections.emptyList();
    }
}
