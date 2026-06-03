package cn.oa.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("doc_dispatch")
public class DocDispatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String serialNo;

    private String title;

    private String securityLevel;

    private String urgency;

    private String issuingOrg;

    private String mainRecipient;

    private String ccRecipient;

    private Long drafterId;

    private LocalDate draftDate;

    private Long reviewerId;

    private String countersignerIds;

    private Long signerId;

    private String contentLink;

    private String attachmentIds;

    private String status;

    private Long processInstanceId;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}