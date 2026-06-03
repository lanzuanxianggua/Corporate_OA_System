package cn.oa.hr.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HR假期余额表
 * 对应表: hr_leave_balance
 *
 * @author oa-hr
 */
@Data
@TableName("hr_leave_balance")
public class HrLeaveBalance {

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 员工ID
     */
    private Long empId;

    /**
     * 假期类型
     */
    private String leaveType;

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
     * 说明：remainingDays 表示账面上的剩余额度，包含已冻结部分。
     * 可申请余额 = remainingDays - frozenDays（由业务层计算，返回给前端时填入 availableDays）
     */
    @TableField(exist = true)
    private BigDecimal remainingDays;

    /**
     * 过期日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;

    /**
     * 状态(ACTIVE/INACTIVE)
     */
    private String status;

    /**
     * 删除标志(0存在 1删除)
     */
    @TableLogic
    private String delFlag;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // ============ 非数据库字段 ============

    /**
     * 员工姓名（关联查询）
     */
    @TableField(exist = false)
    private String empName;

    /**
     * 部门名称（关联查询）
     */
    @TableField(exist = false)
    private String deptName;

    /**
     * 假期类型名称（字典翻译）
     */
    @TableField(exist = false)
    private String leaveTypeName;

    /**
     * 可用余额（非数据库字段，由业务层计算）
     * 计算公式：availableDays = remainingDays - frozenDays
     * 含义：员工当前实际可用于新申请的假期天数
     */
    @TableField(exist = false)
    private BigDecimal availableDays;
}
