package cn.oa.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fin_loan_repayment")
public class FinLoanRepayment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long loanId;

    private BigDecimal amount;

    private LocalDateTime repayTime;

    private String remark;
}