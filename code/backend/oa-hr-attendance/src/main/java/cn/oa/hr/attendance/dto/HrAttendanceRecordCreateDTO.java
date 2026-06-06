package cn.oa.hr.attendance.dto;
import io.swagger.v3.oas.annotations.media.Schema; import jakarta.validation.constraints.NotNull; import lombok.Data;
import java.time.LocalDate; import java.time.LocalDateTime;
@Data @Schema(description = "打卡请求")
public class HrAttendanceRecordCreateDTO {
    @NotNull @Schema(description = "打卡日期") private LocalDate clockDate;
    @Schema(description = "打卡时间(不传则用服务器时间)") private LocalDateTime clockTime;
    @Schema(description = "方式: GPS/WIFI/MANUAL") private String method;
}