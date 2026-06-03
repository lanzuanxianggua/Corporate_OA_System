package cn.oa.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

    private Boolean isDuplicate;

    private String remark;
}