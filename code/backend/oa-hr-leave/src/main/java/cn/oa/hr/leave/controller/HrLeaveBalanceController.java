package cn.oa.hr.leave.controller;

import cn.oa.hr.leave.entity.HrLeaveBalance;
import cn.oa.hr.leave.service.HrLeaveBalanceService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.util.List;

@Tag(name = "假期余额")
@RestController
@RequestMapping("/api/v1/hr-leave/balances")
@RequiredArgsConstructor
public class HrLeaveBalanceController {

    private final HrLeaveBalanceService balanceService;

    @Data
    public static class InitBalanceDTO {
        @NotNull
        private Long empId;
        @NotBlank
        private String leaveType;
        @NotNull
        private Integer year;
        @NotNull
        @DecimalMin("0.5")
        @DecimalMax("365")
        private BigDecimal totalDays;
    }

    @Data
    public static class AdjustBalanceDTO {
        @NotNull
        @DecimalMin("-365")
        @DecimalMax("365")
        private BigDecimal days;
        @NotBlank
        private String reason;
    }

    @Operation(summary = "查询我的假期余额")
    @GetMapping("/me")
    @RequirePermission("hr-leave:leave-balance:view")
    public R<List<HrLeaveBalance>> myBalances() {
        Long empId = UserContext.get().getEmpId();
        return R.ok(balanceService.listByEmpId(empId));
    }

    @Operation(summary = "初始化假期余额")
    @PostMapping("/actions/init")
    @RequirePermission("hr-leave:leave-balance:init")
    public R<Void> initBalance(@RequestBody InitBalanceDTO dto) {
        balanceService.initBalance(dto.getEmpId(), dto.getLeaveType(), dto.getYear(), dto.getTotalDays());
        return R.ok();
    }

    @Operation(summary = "调整假期余额")
    @PostMapping("/{id}/adjustments")
    @RequirePermission("hr-leave:leave-balance:adjust")
    public R<Void> adjustBalance(@PathVariable Long id, @RequestBody AdjustBalanceDTO dto) {
        balanceService.adjustBalance(id, dto.getDays(), dto.getReason());
        return R.ok();
    }
}
