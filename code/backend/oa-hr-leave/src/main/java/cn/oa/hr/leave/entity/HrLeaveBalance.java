package cn.oa.hr.leave.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 假期余额.
 *
 * <p>对应表 hr_leave_balance, 每个员工每年每种假期类型一条记录.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_leave_balance")
@Schema(description = "假期余额")
public class HrLeaveBalance extends BaseEntity {

    @Schema(description = "员工 emp_id")
    @TableField("emp_id")
    private Long empId;

    @Schema(description = "请假类型: ANNUAL/SICK/PERSONAL/MARRIAGE/MATERNITY")
    @TableField("leave_type")
    private String leaveType;

    @Schema(description = "年度")
    @TableField("year")
    private Integer year;

    @Schema(description = "总额度(天)")
    @TableField("total_days")
    private BigDecimal totalDays;

    @Schema(description = "已用天数")
    @TableField("used_days")
    private BigDecimal usedDays;

    @Schema(description = "冻结天数(审批中)")
    @TableField("frozen_days")
    private BigDecimal frozenDays;

    @Schema(description = "剩余天数")
    @TableField("remaining_days")
    private BigDecimal remainingDays;

    @Schema(description = "状态: ACTIVE/INACTIVE")
    @TableField("status")
    private String status;
}
