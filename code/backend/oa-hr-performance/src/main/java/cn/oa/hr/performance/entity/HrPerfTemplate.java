package cn.oa.hr.performance.entity;
import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data; import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true) @TableName("hr_perf_template") @Schema(description="绩效模板")
public class HrPerfTemplate extends BaseEntity {
    private String templateName; private String description; private String dimensions; private String status;
}