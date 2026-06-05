package cn.oa.hr.leave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 请假查询 DTO.
 */
@Data
@Schema(description = "请假查询参数")
public class HrLeaveQueryDTO {

    @Schema(description = "状态: PENDING/APPROVED/REJECTED/CANCELLED")
    private String status;

    @Schema(description = "请假类型: ANNUAL/SICK/PERSONAL/MARRIAGE/MATERNITY")
    private String leaveType;

    @Schema(description = "页码(从 1 开始)", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
