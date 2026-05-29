package cn.oa.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 工作流任务处理DTO
 */
@Data
public class HandleTaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    @NotNull(message = "taskId不能为空")
    private Long taskId;

    /** 操作状态/动作 (1=通过, 2=拒绝) */
    @NotNull(message = "status不能为空")
    private Integer status;

    /** 审批意见 */
    private String remark;
}
