package cn.oa.meeting.dto;

import lombok.Data;

/**
 * 会议室预订查询DTO
 *
 * @author oa-meeting
 */
@Data
public class MtBookingQueryDTO {

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    /** 会议室ID */
    private Long roomId;

    /** 预订人ID */
    private Long bookEmpId;

    /** 状态 */
    private Integer status;
}
