package cn.oa.task.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论VO
 */
@Data
public class TaskCommentVO {

    private Long id;
    private Long taskId;
    private Long empId;
    private String empName;
    private String content;
    private Long replyToId;
    private String replyToContent;
    private String replyToEmpName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
