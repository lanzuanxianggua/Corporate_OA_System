package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaLeaveBalance;
import cn.oa.service.LeaveBalanceService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave-balance")
@Tag(name = "假期余额管理")
public class LeaveBalanceController {

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @GetMapping("/page")
    @RequireAdmin
    @Operation(summary = "分页查询假期余额")
    public R<PageResult<OaLeaveBalance>> page(@RequestParam int pageNum,
                                               @RequestParam int pageSize,
                                               @RequestParam(required = false) Long empId,
                                               @RequestParam(required = false) Integer year,
                                               @RequestParam(required = false) String searchKey) {
        IPage<OaLeaveBalance> page = leaveBalanceService.pageList(pageNum, pageSize, empId, year, searchKey);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/my")
    @Operation(summary = "查询当前用户假期余额")
    public R<List<OaLeaveBalance>> my(HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        return R.ok(leaveBalanceService.myBalances(empId));
    }

    @PostMapping("/init")
    @RequireAdmin
    @Operation(summary = "初始化员工年度假期余额")
    public R<Void> init(@RequestBody Map<String, Object> params) {
        Long empId = Long.valueOf(params.get("empId").toString());
        Integer year = Integer.valueOf(params.get("year").toString());
        leaveBalanceService.initYearBalance(empId, year);
        return R.ok();
    }
}
