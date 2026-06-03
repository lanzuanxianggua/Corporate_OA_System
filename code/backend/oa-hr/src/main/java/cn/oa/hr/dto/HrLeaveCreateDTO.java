package cn.oa.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * HR请假创建DTO
 *
 * @author oa-hr
 */
@Data
public class HrLeaveCreateDTO {

    /**
     * 假期类型
     */
    @NotBlank(message = "假期类型不能为空")
    private String leaveType;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 请假时段(FULL/AM/PM)
     */
    @NotBlank(message = "请假时段不能为空")
    private String leavePeriod;

    /**
     * 请假原因
     */
    @NotBlank(message = "请假原因不能为空")
    @Size(max = 500, message = "请假原因最长500字")
    private String reason;

    /**
     * 附件列表JSON
     */
    private String attachments;
}
