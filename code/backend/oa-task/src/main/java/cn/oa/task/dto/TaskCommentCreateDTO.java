package cn.oa.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建评论请求")
public class TaskCommentCreateDTO {

    @NotNull(message = "任务ID不能为空")
    @Schema(description = "任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long taskId;

    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "回复目标评论ID")
    private Long replyToId;
}
