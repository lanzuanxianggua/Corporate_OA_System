package cn.oa.hr.training.entity;
import cn.oa.platform.common.base.BaseEntity; import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema; import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_train_course") @Schema(description="培训课程")
public class HrTrainCourse extends BaseEntity {
    private String courseName; private String courseType; private BigDecimal credit;
    private Integer totalHours; private String description; private String status;
}