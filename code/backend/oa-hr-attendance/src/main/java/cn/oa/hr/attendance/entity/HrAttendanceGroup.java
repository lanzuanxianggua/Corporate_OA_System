package cn.oa.hr.attendance.entity;
import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_attendance_group") @Schema(description="考勤组")
public class HrAttendanceGroup extends BaseEntity {
    @Schema(description="考勤组名称") private String groupName;
    @Schema(description="类型: FIXED/FLEXIBLE/SCHEDULE") private String groupType;
    @Schema(description="上班时间") private String clockInTime;
    @Schema(description="下班时间") private String clockOutTime;
    @Schema(description="迟到阈值(分钟)") private Integer lateMinutes;
    @Schema(description="早退阈值(分钟)") private Integer earlyMinutes;
    @Schema(description="工作日 1-7逗号分隔") private String workDays;
    @Schema(description="状态") private String status;
    @Schema(description="部门ID") private Long deptId;
}