package cn.oa.hr.performance.entity;
import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_perf_goal") @Schema(description="绩效目标")
public class HrPerfGoal extends BaseEntity {
    private Long cycleId; private Long empId;
    private String goalContent; private String targetValue; private BigDecimal weight;
    private String status; private BigDecimal score; private String grade;
}