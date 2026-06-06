package cn.oa.hr.recruitment.entity;
import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal; import java.time.LocalDate;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_recruit_offer") @Schema(description="Offer")
public class HrRecruitOffer extends BaseEntity {
    private Long candidateId; private BigDecimal offerSalary; private LocalDate offerDate;
    private LocalDate onboardDate; private String status; private Long wfInstanceId;
    private String rejectReason; private String remark;
}