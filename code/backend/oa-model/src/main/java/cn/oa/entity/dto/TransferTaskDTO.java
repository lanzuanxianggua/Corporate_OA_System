package cn.oa.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 工作流转办DTO
 */
@Data
public class TransferTaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    @NotNull(message = "taskId不能为空")
    private Long taskId;

    /** 转办目标人ID */
    @NotNull(message = "toAssigneeId不能为空")
    private Long toAssigneeId;

    /** 转办原因 */
    private String reason;
}
