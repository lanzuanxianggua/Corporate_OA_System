package cn.oa.hr.performance.entity;
import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_perf_result") @Schema(description="绩效结果")
public class HrPerfResult extends BaseEntity {
    private Long cycleId; private Long empId;
    private BigDecimal totalScore; private String grade; private Integer rank; private String status;
}