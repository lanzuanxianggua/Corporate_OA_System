package cn.oa.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 印章申请创建 DTO.
 */
@Data
@Schema(description = "印章申请创建请求")
public class AdmSealApplyCreateDTO {

    @NotNull(message = "印章不能为空")
    @Schema(description = "印章 ID", example = "1")
    private Long sealId;

    @NotBlank(message = "申请用途不能为空")
    @Schema(description = "申请用途", example = "合同盖章")
    private String purpose;

    @NotBlank(message = "用印文件不能为空")
    @Schema(description = "用印文件名称", example = "销售合同V1.2.pdf")
    private String docName;

    @NotNull(message = "文件份数不能为空")
    @Schema(description = "文件份数", example = "3")
    private Integer docCount;

    @Schema(description = "期望用印日期", example = "2026-06-10")
    private LocalDate expectDate;

    @Schema(description = "备注")
    private String remark;
}
