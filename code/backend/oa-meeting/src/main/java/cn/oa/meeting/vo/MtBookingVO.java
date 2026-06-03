package cn.oa.meeting.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议室预订VO
 *
 * @author oa-meeting
 */
@Data
public class MtBookingVO {

    private Long id;

    /** 会议室ID */
    private Long roomId;

    /** 会议室名称 */
    private String roomName;

    /** 会议标题 */
    private String title;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 参与人列表(JSON) */
    private String participants;

    /** 状态(0=待审批 1=已通过 2=已拒绝 3=已取消) */
    private Integer status;

    /** 状态名称 */
    private String statusName;

    /** 预订人ID */
    private Long bookEmpId;

    /** 预订人姓名 */
    private String bookEmpName;

    /** 工作流实例ID */
    private Long processInstanceId;

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
