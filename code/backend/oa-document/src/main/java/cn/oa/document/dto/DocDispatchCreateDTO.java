package cn.oa.document.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发文创建DTO
 *
 * @author oa-document
 */
@Data
public class DocDispatchCreateDTO {

    /** 文件标题 */
    @NotBlank(message = "文件标题不能为空")
    private String title;

    /** 密级: NORMAL-普通 SECRET-秘密 CONFIDENTIAL-机密 */
    private String securityLevel;

    /** 紧急程度: NORMAL-普通 URGENT-紧急 IMMEDIATE-特急 */
    private String urgency;

    /** 签发机关 */
    private String issuingOrg;

    /** 主送机关 */
    private String mainRecipient;

    /** 抄送机关 */
    private String ccRecipient;

    /** 正文附件路径 */
    private String contentLink;
}
