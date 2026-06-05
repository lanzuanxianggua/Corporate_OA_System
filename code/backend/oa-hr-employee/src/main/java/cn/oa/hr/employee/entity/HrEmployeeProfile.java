package cn.oa.hr.employee.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 员工档案 (HR 扩展视图).
 *
 * <p>1 对 1 关联 sys_employee (HR 业务专属字段: 工号/入职/合同/紧急联系人/银行账号).
 * 区别于 sys_employee 平台层: 本表是 HR 业务视角, 平台层是认证视图.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_profile")
@Schema(description = "员工档案")
public class HrEmployeeProfile extends BaseEntity {

    @Schema(description = "关联 sys_employee.id")
    @TableField("emp_id")
    private Long empId;

    @Schema(description = "工号 (HR 业务编号, 区别于 sys_employee.emp_no)")
    @TableField("work_no")
    private String workNo;

    @Schema(description = "入职日期")
    @TableField("hire_date")
    private LocalDate hireDate;

    @Schema(description = "合同类型: PROBATION/REGULAR/CONTRACT/INTERN")
    @TableField("contract_type")
    private String contractType;

    @Schema(description = "合同到期日")
    @TableField("contract_end_date")
    private LocalDate contractEndDate;

    @Schema(description = "紧急联系人")
    @TableField("emergency_contact")
    private String emergencyContact;

    @Schema(description = "紧急联系电话")
    @TableField("emergency_phone")
    private String emergencyPhone;

    @Schema(description = "开户行")
    @TableField("bank_name")
    private String bankName;

    @Schema(description = "银行卡号")
    @TableField("bank_account")
    private String bankAccount;

    @Schema(description = "状态: ACTIVE/LEAVE/RESIGN")
    @TableField("status")
    private String status;
}
