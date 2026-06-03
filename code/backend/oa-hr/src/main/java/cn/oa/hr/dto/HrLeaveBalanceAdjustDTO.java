package cn.oa.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * HR假期余额调整DTO
 *
 * @author oa-hr
 */
@Data
public class HrLeaveBalanceAdjustDTO {

    /**
     * 余额ID
     */
    @NotNull(message = "余额ID不能为空")
    private Long id;

    /**
     * 调整类型(ADD增加/SUB减少/SET设置为)
     */
    @NotBlank(message = "调整类型不能为空")
    private String adjustType;

    /**
     * 调整天数
     */
    @NotNull(message = "调整天数不能为空")
    private BigDecimal adjustDays;

    /**
     * 调整原因
     */
    @NotBlank(message = "调整原因不能为空")
    private String reason;
}
