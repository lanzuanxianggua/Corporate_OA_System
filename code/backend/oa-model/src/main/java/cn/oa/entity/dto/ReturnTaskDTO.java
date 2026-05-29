package cn.oa.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 工作流退回DTO
 */
@Data
public class ReturnTaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    @NotNull(message = "taskId不能为空")
    private Long taskId;

    /** 退回目标节点 */
    @NotBlank(message = "returnTarget不能为空")
    private String returnTarget;

    /** 退回意见 */
    private String remark;
}
