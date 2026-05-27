package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oa_loan")
public class OaLoan {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long empId;

    private BigDecimal loanAmount;

    private String loanReason;

    private String repaymentPlan;

    private Integer status = 0;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private String empName;
}