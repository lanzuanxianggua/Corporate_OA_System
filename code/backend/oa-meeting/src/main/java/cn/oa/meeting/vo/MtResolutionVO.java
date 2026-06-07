package cn.oa.meeting.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会议决议 VO.
 */
@Data
@Schema(description = "会议决议视图")
public class MtResolutionVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "会议 ID")
    private Long meetingId;

    @Schema(description = "会议标题")
    private String meetingTitle;

    @Schema(description = "决议标题")
    private String title;

    @Schema(description = "决议内容")
    private String content;

    @Schema(description = "责任人 emp_id")
    private Long assigneeId;

    @Schema(description = "责任人姓名")
    private String assigneeName;

    @Schema(description = "截止日期")
    private LocalDate deadline;

    @Schema(description = "优先级: HIGH/NORMAL/LOW")
    private String priority;

    @Schema(description = "状态: PENDING/IN_PROGRESS/COMPLETED/OVERDUE")
    private String status;

    @Schema(description = "完成时间")
    private LocalDateTime completeTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
