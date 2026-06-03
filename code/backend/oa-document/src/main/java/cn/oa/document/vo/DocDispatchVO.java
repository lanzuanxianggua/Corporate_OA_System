package cn.oa.document.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发文VO
 *
 * @author oa-document
 */
@Data
public class DocDispatchVO {

    private Long id;

    /** 文号 */
    private String serialNo;

    /** 文件标题 */
    private String title;

    /** 密级 */
    private String securityLevel;

    /** 密级名称 */
    private String securityLevelName;

    /** 紧急程度 */
    private String urgency;

    /** 紧急程度名称 */
    private String urgencyName;

    /** 签发机关 */
    private String issuingOrg;

    /** 主送机关 */
    private String mainRecipient;

    /** 抄送机关 */
    private String ccRecipient;

    /** 拟稿人ID */
    private Long drafterId;

    /** 拟稿人姓名 */
    private String drafterName;

    /** 拟稿日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate draftDate;

    /** 核稿人ID */
    private Long reviewerId;

    /** 核稿人姓名 */
    private String reviewerName;

    /** 签发人ID */
    private Long signerId;

    /** 签发人姓名 */
    private String signerName;

    /** 正文附件路径 */
    private String contentLink;

    /** 状态 */
    private String status;

    /** 状态名称 */
    private String statusName;

    /** 签发日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dispatchDate;

    /** 工作流实例ID */
    private Long processInstanceId;

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
