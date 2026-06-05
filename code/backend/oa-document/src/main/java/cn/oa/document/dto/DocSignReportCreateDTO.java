package cn.oa.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 签报创建 DTO.
 */
@Data
@Schema(description = "签报创建请求")
public class DocSignReportCreateDTO {

    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题", example = "关于购置办公设备的请示")
    private String title;

    @NotBlank(message = "签报类型不能为空")
    @Schema(description = "签报类型", example = "请示")
    private String reportType;

    @Schema(description = "签报内容")
    private String content;
}
