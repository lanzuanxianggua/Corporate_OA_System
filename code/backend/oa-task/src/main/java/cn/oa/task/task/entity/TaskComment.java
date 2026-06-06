package cn.oa.task.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 任务评论.
 *
 * <p>对应表 task_comments.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_comments")
@Schema(description = "任务评论")
public class TaskComment extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "任务 id")
    @TableField("item_id")
    private Long itemId;

    @Schema(description = "评论内容")
    @TableField("content")
    private String content;

    @Schema(description = "评论人 emp_id")
    @TableField("emp_id")
    private Long empId;

    @Schema(description = "回复的评论 id")
    @TableField("parent_comment_id")
    private Long parentCommentId;
}
