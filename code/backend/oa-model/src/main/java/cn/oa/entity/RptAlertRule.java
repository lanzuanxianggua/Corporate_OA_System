package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rpt_alert_rule")
public class RptAlertRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    private String ruleType;

    private String metric;

    private String conditionType;

    @NotNull(message = "阈值不能为空")
    private BigDecimal threshold;

    private BigDecimal thresholdMax;

    private String checkCron;

    private String notifyType = "inner";

    private String notifyTargets;

    private Character status = '0';

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer delFlag;
}
