package cn.oa.meeting.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议室 VO.
 */
@Data
@Schema(description = "会议室详情")
public class MtRoomVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "会议室名称")
    private String roomName;

    @Schema(description = "会议室编码")
    private String roomCode;

    @Schema(description = "所在楼层")
    private String floor;

    @Schema(description = "容纳人数")
    private Integer capacity;

    @Schema(description = "设施")
    private String facility;

    @Schema(description = "位置描述")
    private String location;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "今日预约数")
    private Integer todayBookings;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
