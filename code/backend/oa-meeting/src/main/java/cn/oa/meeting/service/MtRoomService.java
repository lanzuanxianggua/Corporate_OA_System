package cn.oa.meeting.service;

import cn.oa.meeting.dto.MtRoomSaveDTO;
import cn.oa.meeting.entity.MtRoom;
import cn.oa.meeting.vo.MtRoomVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议室服务接口
 *
 * @author oa-meeting
 */
public interface MtRoomService {

    /**
     * 创建会议室
     *
     * @param dto   会议室保存DTO
     * @param empId 操作人ID
     * @return 会议室ID
     */
    Long create(MtRoomSaveDTO dto, Long empId);

    /**
     * 更新会议室
     *
     * @param id    会议室ID
     * @param dto   会议室保存DTO
     * @param empId 操作人ID
     */
    void update(Long id, MtRoomSaveDTO dto, Long empId);

    /**
     * 删除会议室
     *
     * @param id 会议室ID
     */
    void delete(Long id);

    /**
     * 查询会议室详情
     *
     * @param id 会议室ID
     * @return 会议室VO
     */
    MtRoomVO getDetail(Long id);

    /**
     * 查询所有会议室列表
     *
     * @return 会议室VO列表
     */
    List<MtRoomVO> listAll();

    /**
     * 查询可用会议室（指定时间段未被预订）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 可用会议室VO列表
     */
    List<MtRoomVO> listAvailable(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * entity转VO
     *
     * @param entity MtRoom实体
     * @return MtRoomVO
     */
    MtRoomVO toVO(MtRoom entity);
}
