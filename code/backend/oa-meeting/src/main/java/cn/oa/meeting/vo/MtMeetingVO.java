package cn.oa.meeting.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议记录 VO.
 */
@Data
@Schema(description = "会议记录视图")
public class MtMeetingVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关联预约 ID")
    private Long bookingId;

    @Schema(description = "会议标题")
    private String meetingTitle;

    @Schema(description = "会议内容/纪要")
    private String summary;

    @Schema(description = "实际开始时间")
    private LocalDateTime startTime;

    @Schema(description = "实际结束时间")
    private LocalDateTime endTime;

    @Schema(description = "会议室")
    private String location;

    @Schema(description = "组织者 emp_id")
    private Long organizerId;

    @Schema(description = "组织者姓名")
    private String organizerName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
