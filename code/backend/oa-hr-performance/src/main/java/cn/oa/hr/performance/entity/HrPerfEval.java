package cn.oa.hr.performance.entity;
import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_perf_eval") @Schema(description="绩效评估")
public class HrPerfEval extends BaseEntity {
    private Long goalId; private Long evaluatorId; private String evalType;
    private BigDecimal score; private String comment; private String status;
}