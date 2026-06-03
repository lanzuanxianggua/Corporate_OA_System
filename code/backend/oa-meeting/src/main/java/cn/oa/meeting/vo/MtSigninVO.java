package cn.oa.meeting.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会议签到VO
 *
 * @author oa-meeting
 */
@Data
public class MtSigninVO {

    private Long id;

    /** 预订ID */
    private Long bookingId;

    /** 员工ID */
    private Long empId;

    /** 员工姓名 */
    private String empName;

    /** 签到时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime signinTime;

    /** 签到类型(0=正常签到 1=补签) */
    private Integer signinType;

    /** 签到类型名称 */
    private String signinTypeName;

    /** 签到位置 */
    private String location;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
