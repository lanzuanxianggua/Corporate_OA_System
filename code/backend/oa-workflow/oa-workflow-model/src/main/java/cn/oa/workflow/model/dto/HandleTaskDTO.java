package cn.oa.workflow.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 处理任务请求
 */
@Data
public class HandleTaskDTO {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    /** 处理结果: APPROVED/REJECTED */
    @NotBlank(message = "处理结果不能为空")
    private String result;

    /** 审批意见 */
    private String opinion;

    /** 手写签批图片URL */
    private String signature;
}