package cn.oa.meeting.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会议决议VO
 *
 * @author oa-meeting
 */
@Data
public class MtResolutionVO {

    private Long id;

    /** 预订ID */
    private Long bookingId;

    /** 会议标题 */
    private String bookingTitle;

    /** 决议内容 */
    private String content;

    /** 负责人ID */
    private Long assigneeId;

    /** 负责人姓名 */
    private String assigneeName;

    /** 截止日期 */
    private LocalDate dueDate;

    /** 状态(0=待办 1=进行中 2=已完成 3=已逾期) */
    private Integer status;

    /** 状态名称 */
    private String statusName;

    /** 关联任务ID */
    private Long taskId;

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
