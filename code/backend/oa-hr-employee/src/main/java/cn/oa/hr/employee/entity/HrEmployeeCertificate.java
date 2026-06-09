package cn.oa.hr.employee.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_certificate")
public class HrEmployeeCertificate extends BaseEntity {
    private Long empId;
    private String certificateName;
    private String certificateNo;
    private String issueOrg;
    private LocalDate issueDate;
    private LocalDate expireDate;
    private String attachmentUrl;
    private String status;
}
