package cn.oa.hr.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * HR假期规则表
 * 对应表: hr_leave_rule
 *
 * @author oa-hr
 */
@Data
@TableName("hr_leave_rule")
public class HrLeaveRule {

    /**
     * 规则ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 假期类型
     */
    private String leaveType;

    /**
     * 最小请假单位(天)
     */
    private BigDecimal minUnit;

    /**
     * 单次最大天数
     */
    private BigDecimal maxDaysPerApply;

    /**
     * 是否扣减余额(0否 1是)
     */
    private Integer deductBalance;

    /**
     * 是否扣薪(0否 1是)
     */
    private Integer deductSalary;

    /**
     * 是否需要附件(0否 1是)
     */
    private Integer requireAttachment;

    /**
     * 规则脚本(Groovy/SpEL)
     */
    private String ruleScript;

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
}
