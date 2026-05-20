package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.R;
import cn.oa.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "数据统计")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/dashboard")
    @RequireAdmin
    @Operation(summary = "获取仪表盘统计数据")
    public R<Map<String, Object>> dashboard(
            @RequestParam(defaultValue = "today") String period,
            @RequestParam(required = false) Integer year) {
        Map<String, Object> data = statisticsService.getDashboardStats(period, year);
        return R.ok(data);
    }
}
