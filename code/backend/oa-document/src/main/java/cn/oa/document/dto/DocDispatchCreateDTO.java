package cn.oa.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发文创建 DTO.
 */
@Data
@Schema(description = "发文创建请求")
public class DocDispatchCreateDTO {

    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题", example = "关于2026年度工作总结的通知")
    private String title;

    @Schema(description = "主题词", example = "年度总结 通知")
    private String subjectWord;

    @Schema(description = "发送部门", example = "各部门")
    private String sendToDept;

    @Schema(description = "抄送部门", example = "总经理办公室")
    private String copyToDept;

    @Schema(description = "紧急程度: URGENT/EMERGENCY/NORMAL", example = "NORMAL")
    private String urgency;

    @Schema(description = "密级: TOP_SECRET/SECRET/CONFIDENTIAL/NORMAL", example = "NORMAL")
    private String securityLevel;

    @Schema(description = "正文内容(富文本)")
    private String content;
}
