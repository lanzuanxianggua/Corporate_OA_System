package cn.oa.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("doc_receive")
public class DocReceive {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fromOrg;

    private String originalSerial;

    private LocalDate receiveDate;

    private String title;

    private String securityLevel;

    private String urgency;

    private Integer copyCount;

    private String proposedOpinion;

    private String approvedOpinion;

    private String handledOpinion;

    private String circulationRecord;

    private String attachmentId;

    private String status;

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