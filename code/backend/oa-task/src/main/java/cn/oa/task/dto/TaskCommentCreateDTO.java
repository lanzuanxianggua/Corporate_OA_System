package cn.oa.task.dto;
import io.swagger.v3.oas.annotations.media.Schema; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull; import lombok.Data;
@Data @Schema(description = "添加评论请求")
public class TaskCommentCreateDTO {
    @NotNull @Schema(description = "任务ID") private Long itemId;
    @NotBlank @Schema(description = "评论内容") private String content;
    @Schema(description = "回复评论ID") private Long parentCommentId;
}