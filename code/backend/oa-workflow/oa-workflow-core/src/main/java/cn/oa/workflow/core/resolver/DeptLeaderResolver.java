package cn.oa.workflow.core.resolver;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 部门负责人解析器 - 解析发起人所在部门的负责人
 * ruleValue: 可选，部门层级（默认为发起人直接部门）
 *            如 "1" 表示发起人直属部门，"2" 表示上级部门
 */
@Slf4j
@Component
@Order(30)
public class DeptLeaderResolver implements AssigneeResolver {

    // TODO: 注入 DeptService 或 DeptMapper
    // @Autowired
    // private DeptService deptService;

    @Override
    public String getRuleType() {
        return "DEPT_LEADER";
    }

    @Override
    public List<Long> resolve(String ruleValue, Long starterId, String formDataSnapshot) {
        if (starterId == null) {
            log.warn("DeptLeaderResolver: starterId is null");
            return Collections.emptyList();
        }

        // 解析层级，默认为1（直属部门）
        int level = 1;
        if (ruleValue != null && !ruleValue.isBlank()) {
            try {
                level = Integer.parseInt(ruleValue);
            } catch (NumberFormatException e) {
                // ruleValue 可能是 JSON 配置
                try {
                    var obj = JSONUtil.parseObj(ruleValue);
                    level = obj.getInt("level", 1);
                } catch (Exception ex) {
                    log.warn("DeptLeaderResolver: invalid ruleValue={}", ruleValue);
                }
            }
        }

        log.debug("DeptLeaderResolver: resolving dept leader for starterId={}, level={}",
                starterId, level);

        // TODO: 实现部门负责人解析逻辑
        // 1. 根据 starterId 查询员工所属部门
        // 2. 根据层级向上查找目标部门
        // 3. 返回部门负责人ID

        // Employee emp = employeeService.getById(starterId);
        // Dept targetDept = deptService.findByLevel(emp.getDeptId(), level);
        // if (targetDept != null && targetDept.getLeaderId() != null) {
        //     return Collections.singletonList(targetDept.getLeaderId());
        // }

        log.warn("DeptLeaderResolver: Not implemented yet for starterId={}, level={}",
                starterId, level);
        return Collections.emptyList();
    }
}
