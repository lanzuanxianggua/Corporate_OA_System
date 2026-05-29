package cn.oa.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用审批操作DTO - 用于请假/出差/外出/采购/经费/加班/借支等业务审批
 */
@Data
public class ApproveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务ID */
    @NotNull(message = "业务ID不能为空")
    private Long id;

    /** 审批状态 (1=通过, 2=拒绝) */
    @NotNull(message = "审批状态不能为空")
    private Integer status;

    /** 审批意见 */
    private String remark;

    /** 工作流任务ID */
    private Long taskId;
}
