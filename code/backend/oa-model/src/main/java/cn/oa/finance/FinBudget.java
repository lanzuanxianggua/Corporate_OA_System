package cn.oa.finance;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fin_budget")
public class FinBudget {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long deptId;

    private Long projectId;

    private String expenseCategory;

    private Integer year;

    private Integer month;

    private BigDecimal amount;

    private BigDecimal occupiedAmount;

    private BigDecimal executedAmount;

    private String controlStrategy;

    private String status;

    @Version
    private Integer version;

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