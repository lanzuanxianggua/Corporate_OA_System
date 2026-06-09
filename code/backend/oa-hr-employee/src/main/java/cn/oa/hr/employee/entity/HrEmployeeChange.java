package cn.oa.hr.employee.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_change")
public class HrEmployeeChange extends BaseEntity {
    private Long empId;
    private String changeType;
    private Long beforeDeptId;
    private Long afterDeptId;
    private String beforePost;
    private String afterPost;
    private LocalDate effectiveDate;
    private String reason;
    private String status;
}
