package cn.oa.hr.attendance.entity;
import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate; import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_attendance_exception") @Schema(description="考勤异常")
public class HrAttendanceException extends BaseEntity {
    private Long recordId; private Long empId; private LocalDate exceptionDate;
    private String exceptionType; private String status; private String reason;
    private String appealContent; private LocalDateTime appealTime;
    private Long handleEmpId; private String handleComment; private LocalDateTime handleTime;
}