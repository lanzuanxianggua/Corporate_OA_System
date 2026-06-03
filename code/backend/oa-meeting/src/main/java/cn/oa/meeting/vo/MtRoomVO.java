package cn.oa.meeting.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议室VO
 *
 * @author oa-meeting
 */
@Data
public class MtRoomVO {

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

    /** 状态名称 */
    private String statusName;

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
