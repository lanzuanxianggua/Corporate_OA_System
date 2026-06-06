package cn.oa.hr.attendance.dto;
import io.swagger.v3.oas.annotations.media.Schema; import lombok.Data;
import java.time.LocalDate;
@Data @Schema(description = "考勤查询参数")
public class HrAttendanceQueryDTO {
    private Long empId; private String status; private LocalDate startDate; private LocalDate endDate;
    private Integer pageNum = 1; private Integer pageSize = 10;
}