package cn.oa.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("fin_expense_detail")
public class FinExpenseDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long expenseId;

    private LocalDate expenseDate;

    private String expenseType;

    private BigDecimal amount;

    private String invoiceNo;

    private String invoiceImage;

    private String ocrResult;

    private Integer isDuplicate;

    private String remark;
}