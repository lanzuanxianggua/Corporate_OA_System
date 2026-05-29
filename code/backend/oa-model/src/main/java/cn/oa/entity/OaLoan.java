package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oa_loan")
public class OaLoan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private BigDecimal loanAmount;

    private String loanReason;

    private String repaymentPlan;

    private String status = "0";

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    private String delFlag;

    @TableField(exist = false)
    private String empName;

    @TableField(exist = false)
    private Long processInstanceId;
}