package cn.oa.hr.recruitment.entity;
import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_recruit_interview") @Schema(description="面试记录")
public class HrRecruitInterview extends BaseEntity {
    private Long candidateId; private Integer round; private LocalDateTime interviewDate;
    private Long interviewerId; private BigDecimal score; private String evaluation; private String result;
}