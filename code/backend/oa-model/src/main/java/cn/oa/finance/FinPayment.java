package cn.oa.finance;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("fin_payment")
public class FinPayment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contractId;

    private String payee;

    private BigDecimal amount;

    private LocalDate payDate;

    private String payType;

    private String bankAccount;

    private String status;

    private Long processInstanceId;

    private String remark;

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