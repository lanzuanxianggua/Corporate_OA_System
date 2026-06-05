package cn.oa.hr.leave.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假申请单.
 *
 * <p>对应表 hr_leave, 与 wf_instance 通过 businessKey 关联.
 * 状态: PENDING/APPROVED/REJECTED/CANCELLED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_leave")
@Schema(description = "请假申请单")
public class HrLeave extends BaseEntity {

    @Schema(description = "申请人 emp_id")
    @TableField("emp_id")
    private Long empId;

    @Schema(description = "请假类型: ANNUAL/SICK/PERSONAL/MARRIAGE/MATERNITY")
    @TableField("leave_type")
    private String leaveType;

    @Schema(description = "开始日期")
    @TableField("start_date")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    @TableField("end_date")
    private LocalDate endDate;

    @Schema(description = "请假天数")
    @TableField("total_days")
    private java.math.BigDecimal totalDays;

    @Schema(description = "请假事由")
    @TableField("reason")
    private String reason;

    @Schema(description = "状态: PENDING/APPROVED/REJECTED/CANCELLED")
    @TableField("status")
    private String status;

    @Schema(description = "流程实例 ID")
    @TableField("wf_instance_id")
    private Long wfInstanceId;

    @Schema(description = "提交时间")
    @TableField(value = "submit_time", exist = false)
    private LocalDateTime submitTime;
}
