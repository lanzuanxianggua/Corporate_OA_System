package cn.oa.finance.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_payment")
public class FinPayment extends BaseEntity {
    private String paymentNo;
    private Long contractId;
    private Long expenseId;
    private String payee;
    private BigDecimal amount;
    private LocalDate plannedDate;
    private LocalDateTime paidTime;
    private String payMethod;
    private String status;
    private String remark;
}
