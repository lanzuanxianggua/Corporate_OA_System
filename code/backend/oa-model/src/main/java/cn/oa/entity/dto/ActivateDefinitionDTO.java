package cn.oa.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 流程定义激活/停用DTO
 */
@Data
public class ActivateDefinitionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 流程定义ID */
    @NotNull(message = "id不能为空")
    private Long id;
}
