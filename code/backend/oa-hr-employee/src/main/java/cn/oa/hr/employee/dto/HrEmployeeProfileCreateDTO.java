package cn.oa.hr.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 员工档案创建 DTO.
 */
@Data
@Schema(description = "员工档案创建请求")
public class HrEmployeeProfileCreateDTO {

    @NotNull(message = "员工ID不能为空")
    @Schema(description = "关联 sys_employee.id", example = "1")
    private Long empId;

    @Schema(description = "工号", example = "HR20260001")
    private String workNo;

    @Schema(description = "入职日期", example = "2026-06-01")
    private LocalDate hireDate;

    @Schema(description = "合同类型: PROBATION/REGULAR/CONTRACT/INTERN", example = "REGULAR")
    private String contractType;

    @Schema(description = "合同到期日", example = "2029-06-01")
    private LocalDate contractEndDate;

    @Schema(description = "紧急联系人", example = "张三")
    private String emergencyContact;

    @Schema(description = "紧急联系电话", example = "13800138000")
    private String emergencyPhone;

    @Schema(description = "开户行", example = "中国工商银行")
    private String bankName;

    @Schema(description = "银行卡号", example = "6222021234567890123")
    private String bankAccount;
}
