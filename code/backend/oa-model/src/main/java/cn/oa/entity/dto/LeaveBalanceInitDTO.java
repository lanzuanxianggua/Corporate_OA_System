package cn.oa.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 假期余额初始化DTO
 */
@Data
public class LeaveBalanceInitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "员工ID不能为空")
    private Long empId;

    @NotNull(message = "年份不能为空")
    private Integer year;
}
