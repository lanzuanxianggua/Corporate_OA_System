package cn.oa.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 费用报销创建DTO
 *
 * @author oa-finance
 */
@Data
public class FinExpenseCreateDTO {

    @NotBlank(message = "报销标题不能为空")
    @Size(max = 200, message = "报销标题最长200字")
    private String title;

    @NotBlank(message = "费用类别不能为空")
    private String category;

    @Size(max = 1000, message = "描述最长1000字")
    private String description;

    private Long relatedTripId;

    private Long relatedLoanId;

    private BigDecimal loanOffsetAmount;

    @NotEmpty(message = "报销明细不能为空")
    @Valid
    private List<FinExpenseDetailDTO> details;

    /**
     * 报销明细行DTO
     */
    @Data
    public static class FinExpenseDetailDTO {

        @NotBlank(message = "费用类型不能为空")
        private String expenseType;

        @NotNull(message = "费用金额不能为空")
        private BigDecimal amount;

        private String expenseDate;

        private String invoiceNo;

        private String invoiceImage;

        private String remark;
    }
}
