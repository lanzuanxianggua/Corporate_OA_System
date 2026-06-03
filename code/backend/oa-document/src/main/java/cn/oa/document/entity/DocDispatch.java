package cn.oa.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发文表
 * 对应表: doc_dispatch
 *
 * @author oa-document
 */
@Data
@TableName("doc_dispatch")
public class DocDispatch {

    /** 发文ID */
    @TableId(type = IdType.AUTO)
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

    /** 拟稿人ID */
    private Long drafterId;

    /** 拟稿日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate draftDate;

    /** 核稿人ID */
    private Long reviewerId;

    /** 签发人ID */
    private Long signerId;

    /** 正文附件路径 */
    private String contentLink;

    /** 状态: DRAFT-草稿 REVIEWING-核稿 SIGNING-待签发 DISPATCHED-已签发 REJECTED-已退回 */
    private String status;

    /** 签发日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dispatchDate;

    /** 工作流实例ID */
    private Long processInstanceId;

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
