package cn.oa.meeting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 会议签到DTO
 *
 * @author oa-meeting
 */
@Data
public class MtSigninDTO {

    /** 预订ID */
    @NotNull(message = "预订ID不能为空")
    private Long bookingId;

    /** 签到类型(0=正常签到 1=补签) */
    private Integer signinType;

    /** 签到位置 */
    private String location;
}
