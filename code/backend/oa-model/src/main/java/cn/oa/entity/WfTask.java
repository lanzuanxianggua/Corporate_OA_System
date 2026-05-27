package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_task")
public class WfTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long instanceId;

    private Long processId;

    private Integer nodeIndex;

    private String nodeName;

    private Long assigneeId;

    /** 0-pending 1-approved 2-rejected 3-transferred 4-canceled 5-returned */
    private String status = "0";

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actionTime;

    private String remark;

    private String actionSource;

    private Long transferFromId;

    private String transferReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    private Integer remindCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRemindTime;

    /** Parent task ID for countersign/orsign */
    private Long parentTaskId;

    /** countersign / orsign / null */
    private String multiType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 业务标题（非数据库字段） */
    @TableField(exist = false)
    private String businessTitle;

    /** 业务类型（非数据库字段） */
    @TableField(exist = false)
    private String businessType;

    /** 流程实例（非数据库字段） */
    @TableField(exist = false)
    private WfProcessInstance instance;

    /** 审批人姓名（非数据库字段） */
    @TableField(exist = false)
    private String assigneeName;
}
