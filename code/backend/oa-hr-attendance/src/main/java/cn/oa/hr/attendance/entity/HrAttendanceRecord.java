package cn.oa.hr.attendance.entity;
import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_attendance_record") @Schema(description="打卡记录")
public class HrAttendanceRecord extends BaseEntity {
    private Long empId; private LocalDate clockDate;
    private LocalDateTime clockInTime; private LocalDateTime clockOutTime;
    private String clockInMethod; private String clockOutMethod;
    private String status; private BigDecimal workHours;
}