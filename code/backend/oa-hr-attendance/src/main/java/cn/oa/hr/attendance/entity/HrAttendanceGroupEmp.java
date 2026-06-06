package cn.oa.hr.attendance.entity;
import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_attendance_group_emp") @Schema(description="考勤组成员")
public class HrAttendanceGroupEmp extends BaseEntity {
    private Long groupId; private Long empId; private LocalDate effectiveDate; private LocalDate expireDate;
}