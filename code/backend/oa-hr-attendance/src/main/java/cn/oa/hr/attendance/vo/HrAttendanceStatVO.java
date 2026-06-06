package cn.oa.hr.attendance.vo;
import io.swagger.v3.oas.annotations.media.Schema; import lombok.Data;
import java.math.BigDecimal;
@Data @Schema(description = "考勤统计")
public class HrAttendanceStatVO {
    private Long id; private Long empId; private String statDate; private String statType;
    private Integer workDays; private Integer actualDays;
    private Integer lateCount; private Integer earlyCount; private Integer absentCount;
    private BigDecimal totalWorkHours;
}