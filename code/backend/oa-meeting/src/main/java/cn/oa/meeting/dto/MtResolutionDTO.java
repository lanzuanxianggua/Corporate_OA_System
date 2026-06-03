package cn.oa.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 会议决议DTO
 *
 * @author oa-meeting
 */
@Data
public class MtResolutionDTO {

    /** 预订ID */
    @NotNull(message = "预订ID不能为空")
    private Long bookingId;

    /** 决议内容 */
    @NotBlank(message = "决议内容不能为空")
    private String content;

    /** 负责人ID */
    @NotNull(message = "负责人不能为空")
    private Long assigneeId;

    /** 截止日期 */
    @NotNull(message = "截止日期不能为空")
    private LocalDate dueDate;

    /** 状态(0=待办 1=进行中 2=已完成 3=已逾期) */
    private Integer status;
}
