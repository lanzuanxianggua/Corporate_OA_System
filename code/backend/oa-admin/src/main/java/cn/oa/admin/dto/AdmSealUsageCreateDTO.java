package cn.oa.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 印章使用申请DTO
 *
 * @author oa-admin
 */
@Data
@Schema(description = "印章使用申请DTO")
public class AdmSealUsageCreateDTO {

    @NotNull(message = "印章ID不能为空")
    @Schema(description = "印章ID")
    private Long sealId;

    @NotBlank(message = "文件名称不能为空")
    @Schema(description = "文件/合同名称")
    private String documentName;

    @NotNull(message = "使用次数不能为空")
    @Schema(description = "使用次数")
    private Integer usageCount;

    @NotNull(message = "使用日期不能为空")
    @Schema(description = "使用日期")
    private LocalDateTime usageDate;

    @NotBlank(message = "用途说明不能为空")
    @Schema(description = "用途说明")
    private String purpose;
}
