package cn.oa.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fin_expense")
public class FinExpense {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private String title;

    private BigDecimal totalAmount;

    private String category;

    private String description;

    private Long relatedTripId;

    private Long relatedLoanId;

    private BigDecimal loanOffsetAmount;

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