package cn.oa.hr.attendance.entity;
import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal; import java.time.LocalDate;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_attendance_stat") @Schema(description="考勤统计")
public class HrAttendanceStat extends BaseEntity {
    private Long empId; private LocalDate statDate; private String statType;
    private Integer workDays; private Integer actualDays;
    private Integer lateCount; private Integer earlyCount; private Integer absentCount;
    private BigDecimal totalWorkHours;
}