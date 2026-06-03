package cn.oa.hr.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequirePermission;
import cn.oa.common.result.R;
import cn.oa.hr.entity.HrLeaveRule;
import cn.oa.hr.service.HrLeaveRuleService;
import cn.oa.hr.vo.HrLeaveRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HR假期规则管理Controller
 *
 * @author oa-hr
 */
@RestController
@RequestMapping("/api/hr/leave-rules")
@Tag(name = "HR假期规则管理")
@Slf4j
@RequiredArgsConstructor
public class HrLeaveRuleController {

    private final HrLeaveRuleService hrLeaveRuleService;

    /**
     * 查询所有活跃的假期规则
     */
    @GetMapping
    @Operation(summary = "查询所有活跃的假期规则")
    @RequirePermission("hr:leave-rule:list")
    public R<List<HrLeaveRuleVO>> listActiveRules() {
        List<HrLeaveRuleVO> rules = hrLeaveRuleService.listActiveRules();
        return R.ok(rules);
    }

    /**
     * 更新假期规则
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新假期规则")
    @OperationLog(module = "HR假期规则管理", operation = "更新假期规则")
    @RequirePermission("hr:leave-rule:update")
    public R<Void> updateRule(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @RequestBody HrLeaveRule rule) {

        rule.setId(id);
        hrLeaveRuleService.updateRule(rule);
        log.info("HR Leave rule updated: id={}", id);
        return R.ok();
    }
}
