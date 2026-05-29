package cn.oa.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class LeaveApplyDTO implements Serializable {

    @NotNull(message = "请假类型不能为空")
    private Integer leaveType;

    @NotBlank(message = "开始时间不能为空")
    private String startDate;

    @NotBlank(message = "结束时间不能为空")
    private String endDate;

    private Integer days;

    @NotBlank(message = "请假原因不能为空")
    private String reason;
}
