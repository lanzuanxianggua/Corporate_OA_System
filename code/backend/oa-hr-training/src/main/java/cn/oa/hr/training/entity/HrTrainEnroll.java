package cn.oa.hr.training.entity;
import cn.oa.platform.common.base.BaseEntity; import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema; import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_train_enroll") @Schema(description="培训报名")
public class HrTrainEnroll extends BaseEntity {
    private Long sessionId, empId; private LocalDateTime enrollTime, signTime;
    private String attendance; private BigDecimal score, creditGranted;
}