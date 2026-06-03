package cn.oa.finance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 费用报销明细VO
 *
 * @author oa-finance
 */
@Data
public class FinExpenseDetailVO {

    private Long id;

    private Long expenseId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expenseDate;

    private String expenseType;

    private BigDecimal amount;

    private String invoiceNo;

    private String invoiceImage;

    private String remark;
}
