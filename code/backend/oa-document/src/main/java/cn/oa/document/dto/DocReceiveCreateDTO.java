package cn.oa.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 收文登记DTO
 *
 * @author oa-document
 */
@Data
public class DocReceiveCreateDTO {

    /** 来文机关 */
    @NotBlank(message = "来文机关不能为空")
    private String fromOrg;

    /** 原发文号 */
    private String originalSerial;

    /** 收文日期 */
    @NotNull(message = "收文日期不能为空")
    private LocalDate receiveDate;

    /** 文件标题 */
    @NotBlank(message = "文件标题不能为空")
    private String title;

    /** 密级: NORMAL-普通 SECRET-秘密 CONFIDENTIAL-机密 */
    private String securityLevel;

    /** 紧急程度: NORMAL-普通 URGENT-紧急 IMMEDIATE-特急 */
    private String urgency;

    /** 份数 */
    private Integer copyCount;

    /** 附件ID */
    private Long attachmentId;
}
