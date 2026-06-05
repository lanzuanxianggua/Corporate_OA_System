package cn.oa.meeting.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会议室.
 *
 * <p>对应表 mt_rooms.
 * 状态: ACTIVE/INACTIVE/MAINTENANCE
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mt_rooms")
@Schema(description = "会议室")
public class MtRoom extends BaseEntity {

    @Schema(description = "会议室名称")
    @TableField("room_name")
    private String roomName;

    @Schema(description = "会议室编码")
    @TableField("room_code")
    private String roomCode;

    @Schema(description = "所在楼层")
    @TableField("floor")
    private String floor;

    @Schema(description = "容纳人数")
    @TableField("capacity")
    private Integer capacity;

    @Schema(description = "设施(JSON): projector/whiteboard/video_conf/audio")
    @TableField("facility")
    private String facility;

    @Schema(description = "位置描述")
    @TableField("location")
    private String location;

    @Schema(description = "状态: ACTIVE/INACTIVE/MAINTENANCE")
    @TableField("status")
    private String status;
}
