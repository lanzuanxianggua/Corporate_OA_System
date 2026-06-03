package cn.oa.meeting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议室实体
 *
 * @author oa-meeting
 */
@Data
@TableName("mt_room")
public class MtRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会议室名称 */
    private String name;

    /** 容纳人数 */
    private Integer capacity;

    /** 设备列表(JSON) */
    private String devices;

    /** 位置 */
    private String location;

    /** GPS坐标 */
    private String gps;

    /** 状态(0=空闲 1=维修 2=禁用) */
    private Integer status;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
