package cn.oa.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 会议室保存DTO
 *
 * @author oa-meeting
 */
@Data
public class MtRoomSaveDTO {

    /** 会议室名称 */
    @NotBlank(message = "会议室名称不能为空")
    private String name;

    /** 容纳人数 */
    @NotNull(message = "容纳人数不能为空")
    private Integer capacity;

    /** 设备列表(JSON) */
    private String devices;

    /** 位置 */
    @NotBlank(message = "位置不能为空")
    private String location;

    /** GPS坐标 */
    private String gps;

    /** 状态(0=空闲 1=维修 2=禁用) */
    private Integer status;
}
