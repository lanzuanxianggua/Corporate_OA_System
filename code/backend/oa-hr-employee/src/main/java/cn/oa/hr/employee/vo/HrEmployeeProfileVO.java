package cn.oa.hr.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工档案 VO.
 */
@Data
@Schema(description = "员工档案详情")
public class HrEmployeeProfileVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关联 sys_employee.id")
    private Long empId;

    @Schema(description = "员工姓名")
    private String empName;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "所属部门名称")
    private String deptName;

    @Schema(description = "工号")
    private String workNo;

    @Schema(description = "入职日期")
    private LocalDate hireDate;

    @Schema(description = "合同类型")
    private String contractType;

    @Schema(description = "合同到期日")
    private LocalDate contractEndDate;

    @Schema(description = "紧急联系人")
    private String emergencyContact;

    @Schema(description = "紧急联系电话")
    private String emergencyPhone;

    @Schema(description = "开户行")
    private String bankName;

    @Schema(description = "银行卡号")
    private String bankAccount;

    @Schema(description = "状态: ACTIVE/LEAVE/RESIGN")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
