package cn.oa.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "任务评论")
public class TaskCommentVO {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "任务ID")
    private Long itemId;
    @Schema(description = "评论内容")
    private String content;
    @Schema(description = "评论人ID")
    private Long empId;
    @Schema(description = "评论人姓名")
    private String empName;
    @Schema(description = "父评论ID")
    private Long parentCommentId;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
