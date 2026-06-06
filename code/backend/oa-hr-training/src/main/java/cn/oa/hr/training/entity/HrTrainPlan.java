package cn.oa.hr.training.entity;
import cn.oa.platform.common.base.BaseEntity; import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema; import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_train_plan") @Schema(description="培训计划")
public class HrTrainPlan extends BaseEntity {
    private String planName; private Integer year; private Long courseId;
    private BigDecimal totalBudget; private String status;
}