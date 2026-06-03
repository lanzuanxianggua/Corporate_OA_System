package cn.oa.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fin_loan")
public class FinLoan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private BigDecimal loanAmount;

    private BigDecimal repaidAmount;

    private String loanReason;

    private String repaymentPlan;

    private String status;

    private Long processInstanceId;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}