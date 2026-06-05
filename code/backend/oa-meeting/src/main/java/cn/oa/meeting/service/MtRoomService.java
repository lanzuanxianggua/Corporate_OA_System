package cn.oa.meeting.service;

import cn.oa.meeting.dto.MtRoomCreateDTO;
import cn.oa.meeting.dto.MtRoomQueryDTO;
import cn.oa.meeting.entity.MtRoom;
import cn.oa.meeting.mapper.MtRoomMapper;
import cn.oa.meeting.vo.MtRoomVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 会议室业务 Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MtRoomService {

    private final MtRoomMapper mapper;

    /**
     * 创建会议室.
     */
    @Transactional
    public Long create(MtRoomCreateDTO dto) {
        MtRoom room = new MtRoom();
        room.setRoomName(dto.getRoomName());
        room.setRoomCode(dto.getRoomCode());
        room.setFloor(dto.getFloor());
        room.setCapacity(dto.getCapacity());
        room.setFacility(dto.getFacility());
        room.setLocation(dto.getLocation());
        room.setStatus("ACTIVE");
        mapper.insert(room);
        log.info("会议室已创建: roomId={}, name={}", room.getId(), dto.getRoomName());
        return room.getId();
    }

    /**
     * 更新会议室.
     */
    @Transactional
    public void update(Long id, MtRoomCreateDTO dto) {
        MtRoom room = mapper.selectById(id);
        if (room == null) {
            throw new BizException(RCode.NOT_FOUND, "会议室不存在: " + id);
        }
        room.setRoomName(dto.getRoomName());
        room.setRoomCode(dto.getRoomCode());
        room.setFloor(dto.getFloor());
        room.setCapacity(dto.getCapacity());
        room.setFacility(dto.getFacility());
        room.setLocation(dto.getLocation());
        mapper.updateById(room);
        log.info("会议室已更新: roomId={}", id);
    }

    /**
     * 软删除会议室.
     */
    @Transactional
    public void delete(Long id) {
        MtRoom room = mapper.selectById(id);
        if (room == null) {
            throw new BizException(RCode.NOT_FOUND, "会议室不存在: " + id);
        }
        mapper.deleteById(id);
        log.info("会议室已删除: roomId={}", id);
    }

    /**
     * 查询会议室详情.
     */
    public MtRoomVO getById(Long id) {
        Map<String, Object> detail = mapper.selectDetailById(id);
        if (detail == null) {
            throw new BizException(RCode.NOT_FOUND, "会议室不存在: " + id);
        }
        return toVO(detail);
    }

    /**
     * 分页查询会议室列表.
     */
    public PageResult<MtRoomVO> listPage(MtRoomQueryDTO query) {
        Page<MtRoom> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<MtRoom> wrapper = new LambdaQueryWrapper<MtRoom>()
                .eq(query.getStatus() != null, MtRoom::getStatus, query.getStatus())
                .orderByAsc(MtRoom::getRoomName);
        Page<MtRoom> result = mapper.selectPage(page, wrapper);
        return PageResult.of(
                result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(),
                query.getPageNum(),
                query.getPageSize()
        );
    }

    private MtRoomVO toVO(MtRoom room) {
        MtRoomVO vo = new MtRoomVO();
        vo.setId(room.getId());
        vo.setRoomName(room.getRoomName());
        vo.setRoomCode(room.getRoomCode());
        vo.setFloor(room.getFloor());
        vo.setCapacity(room.getCapacity());
        vo.setFacility(room.getFacility());
        vo.setLocation(room.getLocation());
        vo.setStatus(room.getStatus());
        vo.setCreateTime(room.getCreateTime());
        vo.setTodayBookings(0);
        return vo;
    }

    private MtRoomVO toVO(Map<String, Object> map) {
        MtRoomVO vo = new MtRoomVO();
        vo.setId(toLong(map.get("id")));
        vo.setRoomName(toString(map.get("room_name")));
        vo.setRoomCode(toString(map.get("room_code")));
        vo.setFloor(toString(map.get("floor")));
        vo.setCapacity(toInt(map.get("capacity")));
        vo.setFacility(toString(map.get("facility")));
        vo.setLocation(toString(map.get("location")));
        vo.setStatus(toString(map.get("status")));
        vo.setCreateTime(toLocalDateTime(map.get("create_time")));
        vo.setTodayBookings(toInt(map.get("today_bookings")));
        return vo;
    }

    private Long toLong(Object v) {
        return v instanceof Number ? ((Number) v).longValue() : null;
    }

    private Integer toInt(Object v) {
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    private String toString(Object v) {
        return v != null ? v.toString() : null;
    }

    private java.time.LocalDateTime toLocalDateTime(Object v) {
        return v instanceof java.time.LocalDateTime ? (java.time.LocalDateTime) v : null;
    }
}
