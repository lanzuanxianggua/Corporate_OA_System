package cn.oa.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 会议室创建 DTO.
 */
@Data
@Schema(description = "会议室创建请求")
public class MtRoomCreateDTO {

    @NotBlank(message = "会议室名称不能为空")
    @Schema(description = "会议室名称", example = "第一会议室")
    private String roomName;

    @Schema(description = "会议室编码", example = "ROOM-001")
    private String roomCode;

    @Schema(description = "所在楼层", example = "3F")
    private String floor;

    @Schema(description = "容纳人数", example = "20")
    private Integer capacity;

    @Schema(description = "设施(JSON): projector/whiteboard/video_conf/audio",
            example = "{\"projector\":true,\"whiteboard\":true,\"video_conf\":true}")
    private String facility;

    @Schema(description = "位置描述", example = "A栋3楼电梯口右侧")
    private String location;
}
