package cn.oa.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 报销单创建 DTO.
 *
 * <p>包含报销单头信息和多行明细.
 */
@Data
@Schema(description = "报销单创建请求")
public class FinExpenseCreateDTO {

    @NotBlank(message = "报销类型不能为空")
    @Schema(description = "报销类型: TRAVEL/MEAL/OFFICE/OTHER", example = "TRAVEL")
    private String expenseType;

    @NotNull(message = "报销总金额不能为空")
    @Schema(description = "报销总金额", example = "3500.00")
    private BigDecimal totalAmount;

    @NotBlank(message = "报销事由不能为空")
    @Schema(description = "报销事由", example = "出差差旅费")
    private String reason;

    @Valid
    @Schema(description = "费用明细列表")
    private List<ExpenseDetailItem> details;

    /**
     * 费用明细项.
     */
    @Data
    @Schema(description = "费用明细项")
    public static class ExpenseDetailItem {

        @NotNull(message = "费用日期不能为空")
        @Schema(description = "费用日期", example = "2026-06-01")
        private LocalDate feeDate;

        @NotBlank(message = "费用类型不能为空")
        @Schema(description = "费用类型: TRANSPORT/ACCOMMODATION/MEAL/OTHER", example = "TRANSPORT")
        private String feeType;

        @NotNull(message = "金额不能为空")
        @Schema(description = "金额", example = "1200.00")
        private BigDecimal amount;

        @Schema(description = "发票号", example = "INV20260601001")
        private String invoiceNo;

        @Schema(description = "备注", example = "北京-上海高铁")
        private String remark;
    }
}
