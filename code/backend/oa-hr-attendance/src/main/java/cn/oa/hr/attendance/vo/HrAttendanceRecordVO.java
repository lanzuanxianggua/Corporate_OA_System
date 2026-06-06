package cn.oa.hr.attendance.vo;
import io.swagger.v3.oas.annotations.media.Schema; import lombok.Data; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @Schema(description = "打卡记录")
public class HrAttendanceRecordVO {
    private Long id; private Long empId; private String empName; private LocalDate clockDate;
    private LocalDateTime clockInTime; private LocalDateTime clockOutTime;
    private String clockInMethod; private String clockOutMethod; private String status;
}