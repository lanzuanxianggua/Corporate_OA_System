package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.PageParamUtil;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.RptAlertLog;
import cn.oa.entity.RptAlertRule;
import cn.oa.entity.dto.HandleAlertDTO;
import cn.oa.service.AlertLogService;
import cn.oa.service.AlertRuleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
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
        IPage<RptAlertRule> page = alertRuleService.pageList(PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize), ruleType);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/rule")
    @RequireAdmin
    @Operation(summary = "新增预警规则")
    @cn.oa.common.annotation.OperationLog(module = "预警规则", operation = "新增预警规则")
    public R<Void> addRule(@RequestBody @Valid RptAlertRule rule) {
        alertRuleService.save(rule);
        log.info("Alert rule created: id={}", rule.getId());
        return R.ok();
    }

    @PutMapping("/rule")
    @RequireAdmin
    @Operation(summary = "修改预警规则")
    @cn.oa.common.annotation.OperationLog(module = "预警规则", operation = "修改预警规则")
    public R<Void> updateRule(@RequestBody @Valid RptAlertRule rule) {
        alertRuleService.updateById(rule);
        log.info("Alert rule updated: id={}", rule.getId());
        return R.ok();
    }

    @DeleteMapping("/rule/{id}")
    @RequireAdmin
    @Operation(summary = "删除预警规则")
    @cn.oa.common.annotation.OperationLog(module = "预警规则", operation = "删除预警规则")
    public R<Void> deleteRule(@PathVariable Long id) {
        alertRuleService.removeById(id);
        log.info("Alert rule deleted: id={}", id);
        return R.ok();
    }

    @GetMapping("/log/page")
    @RequireAdmin
    @Operation(summary = "分页查询预警日志")
    public R<PageResult<Map<String, Object>>> logPage(@RequestParam int pageNum,
                                                       @RequestParam int pageSize,
                                                       @RequestParam(required = false) Long ruleId,
                                                       @RequestParam(required = false) Character handleStatus) {
        IPage<RptAlertLog> page = alertLogService.pageList(PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize), ruleId, handleStatus);
        Map<Long, RptAlertRule> ruleMap = page.getRecords().stream()
                .map(RptAlertLog::getRuleId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.collectingAndThen(Collectors.toList(), ids -> {
                    if (ids.isEmpty()) {
                        return Map.of();
                    }
                    List<RptAlertRule> rules = alertRuleService.listByIds(ids);
                    if (rules == null || rules.isEmpty()) {
                        return Map.of();
                    }
                    return rules.stream()
                            .collect(Collectors.toMap(RptAlertRule::getId, Function.identity(), (a, b) -> a));
                }));
        List<Map<String, Object>> records = page.getRecords().stream()
                .map(log -> toAlertLogView(log, ruleMap.get(log.getRuleId())))
                .collect(Collectors.toList());
        return R.ok(PageResult.of(page.getTotal(), records));
    }

    @PostMapping("/log/handle/{id}")
    @RequireAdmin
    @Operation(summary = "处理预警")
    @cn.oa.common.annotation.OperationLog(module = "预警规则", operation = "处理预警")
    public R<Void> handleLog(@PathVariable Long id, @RequestBody @Valid HandleAlertDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        alertLogService.handle(id, String.valueOf(empId), dto.getHandleRemark());
        log.info("Alert log handled: id={}, handler={}", id, empId);
        return R.ok();
    }

    private Map<String, Object> toAlertLogView(RptAlertLog log, RptAlertRule rule) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", log.getId());
        item.put("ruleId", log.getRuleId());
        item.put("ruleName", rule != null ? rule.getRuleName() : "-");
        item.put("alertLevel", charToInt(log.getAlertLevel()));
        item.put("level", charToInt(log.getAlertLevel()));
        item.put("metricValue", log.getMetricValue());
        item.put("threshold", log.getThreshold());
        item.put("alertContent", log.getAlertContent());
        item.put("notifyStatus", charToInt(log.getNotifyStatus()));
        item.put("handleStatus", charToInt(log.getHandleStatus()));
        item.put("status", charToInt(log.getHandleStatus()));
        item.put("handler", log.getHandler());
        item.put("handleRemark", log.getHandleRemark());
        item.put("alertTime", log.getAlertTime());
        item.put("createTime", log.getAlertTime());
        item.put("handleTime", log.getHandleTime());
        return item;
    }

    private int charToInt(Character value) {
        if (value == null || !Character.isDigit(value)) {
            return 0;
        }
        return Character.digit(value, 10);
    }
}

