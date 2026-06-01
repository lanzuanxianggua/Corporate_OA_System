package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oa_budget")
public class OaBudget {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long deptId;

    @JsonAlias("year")
    private Integer budgetYear;

    @JsonAlias("month")
    private Integer budgetMonth;

    private BigDecimal amount;

    private BigDecimal usedAmount = BigDecimal.ZERO;

    private String status = "0";

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private String delFlag;
}
