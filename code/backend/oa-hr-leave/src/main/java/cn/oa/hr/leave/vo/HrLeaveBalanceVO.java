package cn.oa.hr.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 假期余额 VO.
 */
@Data
@Schema(description = "假期余额")
public class HrLeaveBalanceVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "员工 emp_id")
    private Long empId;

    @Schema(description = "请假类型")
    private String leaveType;

    @Schema(description = "年度")
    private Integer year;

    @Schema(description = "总额度(天)")
    private BigDecimal totalDays;

    @Schema(description = "已用天数")
    private BigDecimal usedDays;

    @Schema(description = "冻结天数(审批中)")
    private BigDecimal frozenDays;

    @Schema(description = "剩余天数")
    private BigDecimal remainingDays;

    @Schema(description = "状态")
    private String status;
}
