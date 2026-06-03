package cn.oa.document.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收文VO
 *
 * @author oa-document
 */
@Data
public class DocReceiveVO {

    private Long id;

    /** 来文机关 */
    private String fromOrg;

    /** 原发文号 */
    private String originalSerial;

    /** 收文日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate receiveDate;

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

    /** 份数 */
    private Integer copyCount;

    /** 拟办意见 */
    private String proposedOpinion;

    /** 批办人ID */
    private Long approverId;

    /** 批办人姓名 */
    private String approverName;

    /** 批办意见 */
    private String approvedOpinion;

    /** 承办人ID */
    private Long handlerId;

    /** 承办人姓名 */
    private String handlerName;

    /** 承办意见 */
    private String handledOpinion;

    /** 状态 */
    private String status;

    /** 状态名称 */
    private String statusName;

    /** 附件ID */
    private Long attachmentId;

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
