package cn.oa.hr.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.annotation.RequirePermission;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.hr.dto.HrLeaveBalanceAdjustDTO;
import cn.oa.hr.dto.HrLeaveBalanceInitDTO;
import cn.oa.hr.service.HrLeaveBalanceService;
import cn.oa.hr.vo.HrLeaveBalanceVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * HR假期余额管理Controller（管理端）
 *
 * @author oa-hr
 */
@RestController
@RequestMapping("/api/hr/leave-balances")
@Tag(name = "HR假期余额管理")
@Slf4j
@RequiredArgsConstructor
public class HrLeaveBalanceController {

    private final HrLeaveBalanceService hrLeaveBalanceService;

    /**
     * 分页查询假期余额（管理端）
     */
    @GetMapping
    @Operation(summary = "分页查询假期余额")
    @RequirePermission("hr:leave-balance:list")
    public R<PageResult<HrLeaveBalanceVO>> pageQuery(
            @Parameter(description = "员工ID") @RequestParam(required = false) Long empId,
            @Parameter(description = "年度") @RequestParam(required = false) Integer year,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {

        IPage<HrLeaveBalanceVO> page = hrLeaveBalanceService.pageQuery(empId, year, pageNum, pageSize);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    /**
     * 初始化员工年度假期余额
     */
    @PostMapping("/actions/init")
    @Operation(summary = "初始化员工年度假期余额")
    @OperationLog(module = "HR假期余额管理", operation = "初始化假期余额")
    @RequirePermission("hr:leave-balance:init")
    public R<Void> initBalance(@RequestBody @Valid HrLeaveBalanceInitDTO dto) {
        hrLeaveBalanceService.initBalance(dto);
        log.info("HR Leave balance initialized: empId={}, leaveType={}, year={}",
                dto.getEmpId(), dto.getLeaveType(), dto.getYear());
        return R.ok();
    }

    /**
     * 调整假期余额
     */
    @PutMapping("/{id}")
    @Operation(summary = "调整假期余额")
    @OperationLog(module = "HR假期余额管理", operation = "调整假期余额")
    @RequirePermission("hr:leave-balance:update")
    public R<Void> adjustBalance(
            @Parameter(description = "余额ID") @PathVariable Long id,
            @RequestBody @Valid HrLeaveBalanceAdjustDTO dto) {

        // 确保DTO中的ID与路径ID一致
        dto.setId(id);
        hrLeaveBalanceService.adjustBalance(dto);
        log.info("HR Leave balance adjusted: id={}, type={}, days={}",
                id, dto.getAdjustType(), dto.getAdjustDays());
        return R.ok();
    }
}
