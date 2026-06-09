package cn.oa.hr.employee.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_contract")
public class HrEmployeeContract extends BaseEntity {
    private Long empId;
    private String contractNo;
    private String contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate signDate;
    private String status;
    private String remark;
}
