package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.RptAlertLog;
import cn.oa.entity.RptAlertRule;
import cn.oa.service.AlertLogService;
import cn.oa.service.AlertRuleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/alert")
@Tag(name = "预警规则管理")
public class AlertController {

    @Autowired
    private AlertRuleService alertRuleService;

    @Autowired
    private AlertLogService alertLogService;

    @GetMapping("/rule/page")
    @RequireAdmin
    @Operation(summary = "分页查询预警规则")
    public R<PageResult<RptAlertRule>> rulePage(@RequestParam int pageNum,
                                                  @RequestParam int pageSize,
                                                  @RequestParam(required = false) String ruleType) {
        IPage<RptAlertRule> page = alertRuleService.pageList(pageNum, pageSize, ruleType);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/rule")
    @RequireAdmin
    @Operation(summary = "新增预警规则")
    public R<Void> addRule(@RequestBody RptAlertRule rule) {
        alertRuleService.save(rule);
        return R.ok();
    }

    @PutMapping("/rule")
    @RequireAdmin
    @Operation(summary = "修改预警规则")
    public R<Void> updateRule(@RequestBody RptAlertRule rule) {
        alertRuleService.updateById(rule);
        return R.ok();
    }

    @DeleteMapping("/rule/{id}")
    @RequireAdmin
    @Operation(summary = "删除预警规则")
    public R<Void> deleteRule(@PathVariable Long id) {
        alertRuleService.removeById(id);
        return R.ok();
    }

    @GetMapping("/log/page")
    @Operation(summary = "分页查询预警日志")
    public R<PageResult<RptAlertLog>> logPage(@RequestParam int pageNum,
                                                @RequestParam int pageSize,
                                                @RequestParam(required = false) Long ruleId,
                                                @RequestParam(required = false) Character handleStatus) {
        IPage<RptAlertLog> page = alertLogService.pageList(pageNum, pageSize, ruleId, handleStatus);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/log/handle/{id}")
    @Operation(summary = "处理预警")
    public R<Void> handleLog(@PathVariable Long id, @RequestBody Map<String, String> params, HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        String handleRemark = params.get("handleRemark");
        alertLogService.handle(id, String.valueOf(empId), handleRemark);
        return R.ok();
    }
}