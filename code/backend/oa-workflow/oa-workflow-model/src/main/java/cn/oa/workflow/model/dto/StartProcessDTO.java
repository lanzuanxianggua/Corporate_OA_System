package cn.oa.workflow.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 启动流程请求
 */
@Data
public class StartProcessDTO {

    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @NotNull(message = "业务ID不能为空")
    private Long businessId;

    /** 条件上下文（用于条件路由） */
    private Map<String, Object> conditionContext;

    /** 表单数据快照 */
    private Map<String, Object> formData;
}