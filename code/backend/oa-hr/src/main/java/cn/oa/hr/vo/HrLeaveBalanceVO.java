package cn.oa.hr.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HR假期余额VO
 *
 * @author oa-hr
 */
@Data
public class HrLeaveBalanceVO {

    /**
     * ID
     */
    private Long id;

    /**
     * 员工ID
     */
    private Long empId;

    /**
     * 员工姓名
     */
    private String empName;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 假期类型code
     */
    private String leaveType;

    /**
     * 假期类型名称
     */
    private String leaveTypeName;

    /**
     * 年度
     */
    private Integer year;

    /**
     * 总天数
     */
    private BigDecimal totalDays;

    /**
     * 已用天数
     */
    private BigDecimal usedDays;

    /**
     * 审批中冻结天数
     */
    private BigDecimal frozenDays;

    /**
     * 剩余天数（账面余额）
     * 说明：remainingDays 表示账面上的剩余额度，包含已冻结部分
     */
    private BigDecimal remainingDays;

    /**
     * 可用余额（实际可申请新请假的天数）
     * 计算公式：availableDays = remainingDays - frozenDays
     */
    private BigDecimal availableDays;

    /**
     * 过期日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;

    /**
     * 状态
     */
    private String status;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
