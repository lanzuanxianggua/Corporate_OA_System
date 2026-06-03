package cn.oa.hr.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * HR假期规则VO
 *
 * @author oa-hr
 */
@Data
public class HrLeaveRuleVO {

    /**
     * 规则ID
     */
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 假期类型code
     */
    private String leaveType;

    /**
     * 假期类型名称
     */
    private String leaveTypeName;

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
     * 规则脚本
     */
    private String ruleScript;

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
