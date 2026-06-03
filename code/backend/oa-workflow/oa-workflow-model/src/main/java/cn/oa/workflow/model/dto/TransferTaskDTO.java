package cn.oa.workflow.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 转办任务请求
 */
@Data
public class TransferTaskDTO {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "被转办人ID不能为空")
    private Long toAssigneeId;

    private String reason;
}