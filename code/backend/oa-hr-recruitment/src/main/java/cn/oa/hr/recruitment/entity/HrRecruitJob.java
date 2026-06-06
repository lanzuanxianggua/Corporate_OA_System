package cn.oa.hr.recruitment.entity;
import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_recruit_job") @Schema(description="招聘岗位")
public class HrRecruitJob extends BaseEntity {
    private String jobTitle; private Long deptId; private Integer headcount;
    private String requirement; private String responsibility;
    private BigDecimal salaryMin; private BigDecimal salaryMax;
    private String status;
}