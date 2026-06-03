package cn.oa.admin;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("adm_seal_usage")
public class AdmSealUsage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sealId;

    private Long applicantId;

    private String usageReason;

    private String documentName;

    private Long confirmedBy;

    private LocalDateTime confirmedAt;

    private String scannedFile;

    private Long processInstanceId;

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