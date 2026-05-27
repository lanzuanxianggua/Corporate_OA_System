package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rpt_alert_rule")
public class RptAlertRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleName;

    private String ruleType;

    private String metric;

    private String conditionType;

    private BigDecimal threshold;

    private BigDecimal thresholdMax;

    private String checkCron;

    private String notifyType = "inner";

    private String notifyTargets;

    private Character status = '0';

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer delFlag;
}