package cn.oa.hr.training.entity;
import cn.oa.platform.common.base.BaseEntity; import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema; import lombok.Data; import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_train_session") @Schema(description="培训班级")
public class HrTrainSession extends BaseEntity {
    private Long planId; private String sessionName; private LocalDateTime startTime, endTime;
    private String location; private Integer maxCapacity, enrolledNum;
    private String trainer; private String signCode; private String status;
}