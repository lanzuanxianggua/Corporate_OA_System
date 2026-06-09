package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流任务实体。
 *
 * <p>字段对齐 DB 实际 schema (oa_system.wf_task)，V1013 迁移后：
 * <ul>
 *   <li>新增: node_id / task_type / opinion / signature / due_time / complete_time / parent_task_id</li>
 *   <li>废弃: process_id / node_index / action_time / remark / last_remind_time（已 DROP）</li>
 *   <li>status 从 CHAR(1) 改为 VARCHAR(20)，保持数字编码: 0待审批 1已通过 2已驳回 3已转办 4已取消 5已退回</li>
 * </ul>
 * exist=false 字段为 VO 展示字段，不影响 SQL。
 */
@Data
@TableName("wf_task")
public class WfTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long instanceId;

    private Long nodeId;

    private String nodeName;

    private Long assigneeId;

    /** TODO / COUNTERSIGN / PRE_ADD_SIGN / POST_ADD_SIGN */
    private String taskType = "TODO";

    private Long parentTaskId;

    /** 0待审批 / 1已通过 / 2已驳回 / 3已转办 / 4已取消 / 5已退回 */
    private String status = "0";

    private String opinion;

    private String signature;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    private Integer remindCount = 0;

    /** V1010: number of times this task has been auto-escalated due to timeout.
     *  Used by TaskReminderScheduler to cap escalation at 3 to prevent infinite loops. */
    private Integer escalationCount = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ───── VO 显示字段 (非 DB) ─────
    @TableField(exist = false)
    private String actionSource;

    @TableField(exist = false)
    private Long transferFromId;

    @TableField(exist = false)
    private String transferReason;

    @TableField(exist = false)
    private String businessTitle;

    @TableField(exist = false)
    private String businessType;

    @TableField(exist = false)
    private WfProcessInstance instance;

    @TableField(exist = false)
    private String assigneeName;
}
