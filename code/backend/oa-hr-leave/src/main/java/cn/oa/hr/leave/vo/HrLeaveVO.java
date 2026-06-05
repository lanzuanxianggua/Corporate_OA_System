package cn.oa.hr.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假申请 VO.
 */
@Data
@Schema(description = "请假申请详情")
public class HrLeaveVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "申请人 emp_id")
    private Long empId;

    @Schema(description = "申请人姓名")
    private String empName;

    @Schema(description = "所属部门名称")
    private String deptName;

    @Schema(description = "请假类型")
    private String leaveType;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "请假天数")
    private BigDecimal totalDays;

    @Schema(description = "请假事由")
    private String reason;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "流程实例 ID")
    private Long wfInstanceId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
