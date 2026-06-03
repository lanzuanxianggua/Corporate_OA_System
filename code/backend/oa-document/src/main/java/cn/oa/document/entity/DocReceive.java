package cn.oa.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收文表
 * 对应表: doc_receive
 *
 * @author oa-document
 */
@Data
@TableName("doc_receive")
public class DocReceive {

    /** 收文ID */
    @TableId(type = IdType.AUTO)
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

    /** 密级: NORMAL-普通 SECRET-秘密 CONFIDENTIAL-机密 */
    private String securityLevel;

    /** 紧急程度: NORMAL-普通 URGENT-紧急 IMMEDIATE-特急 */
    private String urgency;

    /** 份数 */
    private Integer copyCount;

    /** 拟办意见 */
    private String proposedOpinion;

    /** 批办人ID */
    private Long approverId;

    /** 批办意见 */
    private String approvedOpinion;

    /** 承办人ID */
    private Long handlerId;

    /** 承办意见 */
    private String handledOpinion;

    /** 状态: RECEIVED-已登记 PROPOSED-已拟办 APPROVED-已批办 HANDLING-承办中 ARCHIVED-已归档 */
    private String status;

    /** 附件ID */
    private Long attachmentId;

    /** 删除标志(0存在 1删除) */
    @TableLogic
    private String delFlag;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新人 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
