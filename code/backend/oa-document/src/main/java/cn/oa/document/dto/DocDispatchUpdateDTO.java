package cn.oa.document.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发文更新DTO
 *
 * @author oa-document
 */
@Data
public class DocDispatchUpdateDTO {

    @NotNull(message = "发文ID不能为空")
    private Long id;

    /** 文号 */
    private String serialNo;

    /** 文件标题 */
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

    /** 核稿人ID */
    private Long reviewerId;

    /** 签发人ID */
    private Long signerId;

    /** 正文附件路径 */
    private String contentLink;
}
