package cn.oa.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 会议决议创建 DTO.
 */
@Data
@Schema(description = "会议决议创建请求")
public class MtResolutionCreateDTO {

    @NotNull(message = "关联会议不能为空")
    @Schema(description = "会议 ID")
    private Long meetingId;

    @NotBlank(message = "决议标题不能为空")
    @Schema(description = "决议标题")
    private String title;

    @Schema(description = "决议内容")
    private String content;

    @NotNull(message = "责任人不能为空")
    @Schema(description = "责任人 emp_id")
    private Long assigneeId;

    @Schema(description = "截止日期")
    private LocalDate deadline;

    @Schema(description = "优先级: HIGH/NORMAL/LOW", example = "NORMAL")
    private String priority;
}
