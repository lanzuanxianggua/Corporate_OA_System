package cn.oa.hr.performance.entity;
import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.time.LocalDate;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_perf_cycle") @Schema(description="绩效周期")
public class HrPerfCycle extends BaseEntity {
    private String cycleName; private Long templateId; private Integer year; private Integer quarter;
    private LocalDate startDate; private LocalDate endDate;
    private LocalDate goalStart; private LocalDate goalEnd;
    private LocalDate evalStart; private LocalDate evalEnd;
    private String status;
}