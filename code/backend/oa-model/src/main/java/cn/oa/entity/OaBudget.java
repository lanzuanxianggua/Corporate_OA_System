package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oa_budget")
public class OaBudget {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long deptId;

    private Integer budgetYear;

    private Integer budgetMonth;

    private BigDecimal amount;

    private BigDecimal usedAmount = BigDecimal.ZERO;

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
