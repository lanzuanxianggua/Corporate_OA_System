package cn.oa.workflow.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 退回任务请求
 */
@Data
public class ReturnTaskDTO {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    /** 退回目标: "initiator" 或具体节点编码 */
    @NotBlank(message = "退回目标不能为空")
    private String returnTarget;

    @NotBlank(message = "退回原因不能为空")
    private String remark;
}