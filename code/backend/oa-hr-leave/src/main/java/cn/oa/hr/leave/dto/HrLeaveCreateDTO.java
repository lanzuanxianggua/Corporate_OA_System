package cn.oa.hr.leave.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 请假申请创建 DTO.
 */
@Data
@Schema(description = "请假申请创建请求")
public class HrLeaveCreateDTO {

    @NotBlank(message = "请假类型不能为空")
    @Schema(description = "请假类型: ANNUAL/SICK/PERSONAL/MARRIAGE/MATERNITY", example = "ANNUAL")
    private String leaveType;

    @NotNull(message = "开始日期不能为空")
    @Schema(description = "开始日期", example = "2026-06-10")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    @Schema(description = "结束日期", example = "2026-06-12")
    private LocalDate endDate;

    @Schema(description = "请假事由")
    private String reason;
}
