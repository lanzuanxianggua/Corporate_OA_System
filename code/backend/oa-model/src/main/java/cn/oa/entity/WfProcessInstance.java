package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_process_instance")
public class WfProcessInstance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long processId;

    private String businessType;

    private Long businessId;

    private Long initiatorId;

    private Integer currentNode = 0;

    /** JSON string storing condition context for conditional routing */
    private String conditionContext;

    /** 0-running 1-approved 2-rejected 3-canceled */
    private String status = "0";

    /** Comma-separated active nodeIds for parallel gateway */
    private String activeNodes;

    /** Snapshot of node config at process start time */
    private String snapshotNodeConfig;

    /** Parent instance ID for subprocess nesting */
    private Long parentInstanceId;

    private Integer processVersion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private String delFlag;
}
