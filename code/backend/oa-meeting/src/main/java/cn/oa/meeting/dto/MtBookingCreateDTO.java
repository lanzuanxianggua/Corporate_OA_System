package cn.oa.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议室预订创建DTO
 *
 * @author oa-meeting
 */
@Data
public class MtBookingCreateDTO {

    /** 会议室ID */
    @NotNull(message = "会议室不能为空")
    private Long roomId;

    /** 会议标题 */
    @NotBlank(message = "会议标题不能为空")
    private String title;

    /** 开始时间 */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /** 结束时间 */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /** 参与人列表(JSON) */
    private String participants;
}
