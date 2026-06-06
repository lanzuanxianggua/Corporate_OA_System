package cn.oa.hr.recruitment.entity;
import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_recruit_candidate") @Schema(description="候选人")
public class HrRecruitCandidate extends BaseEntity {
    private Long jobId; private String name; private String phone; private String email;
    private String resumeUrl; private String status; private String source;
    private Long interviewerId; private BigDecimal interviewScore;
}