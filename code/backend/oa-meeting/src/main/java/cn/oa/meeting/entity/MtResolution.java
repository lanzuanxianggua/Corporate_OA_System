package cn.oa.meeting.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会议决议.
 *
 * <p>对应表 mt_resolutions.
 * 状态: PENDING/IN_PROGRESS/COMPLETED/OVERDUE
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mt_resolutions")
@Schema(description = "会议决议")
public class MtResolution extends BaseEntity {

    @Schema(description = "关联会议 ID")
    @TableField("meeting_id")
    private Long meetingId;

    @Schema(description = "决议标题")
    @TableField("title")
    private String title;

    @Schema(description = "决议内容")
    @TableField("content")
    private String content;

    @Schema(description = "负责人 emp_id")
    @TableField("assignee_id")
    private Long assigneeId;

    @Schema(description = "截止日期")
    @TableField("deadline")
    private LocalDateTime deadline;

    @Schema(description = "状态: PENDING/IN_PROGRESS/COMPLETED/OVERDUE")
    @TableField("status")
    private String status;
}
