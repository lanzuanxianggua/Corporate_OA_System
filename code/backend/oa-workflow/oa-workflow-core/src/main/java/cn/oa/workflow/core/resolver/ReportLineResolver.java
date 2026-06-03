package cn.oa.workflow.core.resolver;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 汇报线解析器 - 解析发起人的上级领导
 * ruleValue: 层级数或JSON配置
 *            如 "1" 表示直属上级，"2" 表示上两级领导
 *            或 {"level": 2, "skipEmpty": true} 跳过未设置的上级
 */
@Slf4j
@Component
@Order(40)
public class ReportLineResolver implements AssigneeResolver {

    // TODO: 注入 EmployeeService
    // @Autowired
    // private EmployeeService employeeService;

    @Override
    public String getRuleType() {
        return "REPORT_LINE";
    }

    @Override
    public List<Long> resolve(String ruleValue, Long starterId, String formDataSnapshot) {
        if (starterId == null) {
            log.warn("ReportLineResolver: starterId is null");
            return Collections.emptyList();
        }

        int level = 1;
        boolean skipEmpty = true;

        if (ruleValue != null && !ruleValue.isBlank()) {
            try {
                level = Integer.parseInt(ruleValue);
            } catch (NumberFormatException e) {
                try {
                    var obj = JSONUtil.parseObj(ruleValue);
                    level = obj.getInt("level", 1);
                    skipEmpty = obj.getBool("skipEmpty", true);
                } catch (Exception ex) {
                    log.warn("ReportLineResolver: invalid ruleValue={}", ruleValue);
                }
            }
        }

        log.debug("ReportLineResolver: resolving report line for starterId={}, level={}",
                starterId, level);

        // TODO: 实现汇报线解析逻辑
        // Employee current = employeeService.getById(starterId);
        // for (int i = 0; i < level && current != null; i++) {
        //     if (current.getReportToId() != null) {
        //         current = employeeService.getById(current.getReportToId());
        //     } else if (skipEmpty) {
        //         // 跳过空上级，继续往上找部门负责人
        //         Dept dept = deptService.getById(current.getDeptId());
        //         if (dept != null && dept.getLeaderId() != null) {
        //             current = employeeService.getById(dept.getLeaderId());
        //         } else {
        //             current = null;
        //         }
        //     } else {
        //         current = null;
        //     }
        // }
        // if (current != null) {
        //     return Collections.singletonList(current.getId());
        // }

        log.warn("ReportLineResolver: Not implemented yet for starterId={}, level={}",
                starterId, level);
        return Collections.emptyList();
    }
}
