package cn.oa.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 业务类型+业务ID组合DTO - 用于撤回、催办等操作
 */
@Data
public class BusinessRefDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务类型 (leave/trip/outing/purchase/expense/overtime/loan) */
    @NotBlank(message = "businessType不能为空")
    private String businessType;

    /** 业务ID */
    @NotNull(message = "businessId不能为空")
    private Long businessId;
}
