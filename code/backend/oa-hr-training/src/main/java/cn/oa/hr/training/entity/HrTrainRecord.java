package cn.oa.hr.training.entity;
import cn.oa.platform.common.base.BaseEntity; import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema; import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_train_record") @Schema(description="培训记录")
public class HrTrainRecord extends BaseEntity {
    private Long empId, courseId, sessionId; private BigDecimal totalCredit;
}