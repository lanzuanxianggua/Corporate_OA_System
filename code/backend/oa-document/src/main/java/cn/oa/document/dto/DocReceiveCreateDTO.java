package cn.oa.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 收文创建 DTO.
 */
@Data
@Schema(description = "收文创建请求")
public class DocReceiveCreateDTO {

    @NotBlank(message = "来文单位不能为空")
    @Schema(description = "来文单位", example = "市人力资源和社会保障局")
    private String sourceDept;

    @NotBlank(message = "公文标题不能为空")
    @Schema(description = "公文标题", example = "关于调整社保缴费基数的通知")
    private String docTitle;

    @NotNull(message = "来文日期不能为空")
    @Schema(description = "来文日期", example = "2026-06-01")
    private LocalDate docDate;

    @NotNull(message = "收文日期不能为空")
    @Schema(description = "收文日期", example = "2026-06-05")
    private LocalDate receiveDate;

    @Schema(description = "紧急程度: URGENT/EMERGENCY/NORMAL", example = "NORMAL")
    private String urgentLevel;

    @Schema(description = "内容摘要")
    private String content;

    @Schema(description = "拟办意见")
    private String processOpinion;
}
