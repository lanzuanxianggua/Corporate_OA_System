package cn.oa.meeting.service;

import cn.oa.meeting.dto.MtBookingCreateDTO;
import cn.oa.meeting.dto.MtResolutionDTO;
import cn.oa.meeting.dto.MtSigninDTO;
import cn.oa.meeting.vo.MtBookingVO;
import cn.oa.meeting.vo.MtResolutionVO;
import cn.oa.meeting.vo.MtSigninVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议管理服务接口
 *
 * @author oa-meeting
 */
public interface MtMeetingService {

    /**
     * 预订会议室（含冲突检测）
     *
     * @param dto   预订创建DTO
     * @param empId 预订人ID
     * @return 预订ID
     */
    Long book(MtBookingCreateDTO dto, Long empId);

    /**
     * 检测时间段是否与已有预订冲突
     *
     * @param roomId    会议室ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param excludeId 排除的预订ID（更新时使用）
     * @return true=有冲突
     */
    boolean hasConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long excludeId);

    /**
     * 取消预订
     *
     * @param id    预订ID
     * @param empId 操作人ID
     */
    void cancel(Long id, Long empId);

    /**
     * 会议签到
     *
     * @param dto   签到DTO
     * @param empId 签到人ID
     * @return 签到ID
     */
    Long signin(MtSigninDTO dto, Long empId);

    /**
     * 查询可用会议室（指定时间段未被预订）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 可用会议室VO列表
     */
    List<MtBookingVO> listAvailable(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 分页查询预订列表
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param roomId   会议室ID（可选）
     * @param bookEmpId 预订人ID（可选）
     * @param status   状态（可选）
     * @return 分页结果
     */
    IPage<MtBookingVO> pageQuery(Integer pageNum, Integer pageSize, Long roomId, Long bookEmpId, Integer status);

    /**
     * 查询预订详情
     *
     * @param id 预订ID
     * @return 预订VO
     */
    MtBookingVO getBookingDetail(Long id);

    /**
     * 添加决议
     *
     * @param dto 决议DTO
     * @return 决议ID
     */
    Long addResolution(MtResolutionDTO dto);

    /**
     * 更新决议状态
     *
     * @param id     决议ID
     * @param status 新状态
     */
    void updateResolutionStatus(Long id, Integer status);

    /**
     * 查询预订的决议列表
     *
     * @param bookingId 预订ID
     * @return 决议VO列表
     */
    List<MtResolutionVO> getResolutions(Long bookingId);

    /**
     * 查询预订的签到记录
     *
     * @param bookingId 预订ID
     * @return 签到VO列表
     */
    List<MtSigninVO> getSignins(Long bookingId);
}
