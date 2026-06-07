package cn.oa.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议记录创建 DTO.
 */
@Data
@Schema(description = "会议记录创建请求")
public class MtMeetingCreateDTO {

    @Schema(description = "关联预约 ID")
    private Long bookingId;

    @NotBlank(message = "会议标题不能为空")
    @Schema(description = "会议标题", example = "项目周会")
    private String meetingTitle;

    @Schema(description = "会议内容/纪要")
    private String summary;

    @Schema(description = "实际开始时间")
    private LocalDateTime startTime;

    @Schema(description = "实际结束时间")
    private LocalDateTime endTime;

    @Schema(description = "会议室(冗余)")
    private String location;
}
