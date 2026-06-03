package cn.oa.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * HR假期余额初始化DTO
 *
 * @author oa-hr
 */
@Data
public class HrLeaveBalanceInitDTO {

    /**
     * 员工ID
     */
    @NotNull(message = "员工ID不能为空")
    private Long empId;

    /**
     * 假期类型
     */
    @NotBlank(message = "假期类型不能为空")
    private String leaveType;

    /**
     * 年度
     */
    @NotNull(message = "年度不能为空")
    private Integer year;

    /**
     * 总天数
     */
    @NotNull(message = "总天数不能为空")
    @Positive(message = "总天数必须大于0")
    private BigDecimal totalDays;

    /**
     * 过期日期（可选）
     */
    private String expireDate;
}
