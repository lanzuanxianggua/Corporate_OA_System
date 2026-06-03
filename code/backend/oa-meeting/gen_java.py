start
import os

BASE = r"E:\JavaProject\Corporate_OA_System\code\backend\oa-meeting\src\main\java\cn\oa\meeting"

def w(rel_path, content):
    full = os.path.join(BASE, rel_path.replace("/", os.sep))
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        f.write(content.lstrip("\n"))
    print(f"OK: {rel_path}")

w("service/impl/MtRoomServiceImpl.java", r"""package cn.oa.meeting.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.oa.meeting.dto.MtRoomSaveDTO;
import cn.oa.meeting.entity.MtBooking;
import cn.oa.meeting.entity.MtRoom;
import cn.oa.meeting.mapper.MtBookingMapper;
import cn.oa.meeting.mapper.MtRoomMapper;
import cn.oa.meeting.service.MtRoomService;
import cn.oa.meeting.vo.MtRoomVO;
import cn.oa.platform.core.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MtRoomServiceImpl implements MtRoomService {

    private final MtRoomMapper mtRoomMapper;
    private final MtBookingMapper mtBookingMapper;

    private static final Map<Integer, String> STATUS_MAP = Map.of(
            0, "空闲", 1, "维修中", 2, "已停用"
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MtRoomSaveDTO dto, Long empId) {
        MtRoom room = new MtRoom();
        BeanUtil.copyProperties(dto, room);
        if (room.getStatus() == null) {
            room.setStatus(0);
        }
        mtRoomMapper.insert(room);
        log.info("Created meeting room: id={}, name={}, empId={}", room.getId(), room.getName(), empId);
        return room.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MtRoomSaveDTO dto, Long empId) {
        MtRoom room = mtRoomMapper.selectById(id);
        if (room == null) {
            throw new BusinessException("会议室不存在");
        }
        BeanUtil.copyProperties(dto, room);
        room.setId(id);
        mtRoomMapper.updateById(room);
        log.info("Updated meeting room: id={}, name={}, empId={}", id, room.getName(), empId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MtRoom room = mtRoomMapper.selectById(id);
        if (room == null) {
            throw new BusinessException("会议室不存在");
        }
        mtRoomMapper.deleteById(id);
        log.info("Deleted meeting room: id={}", id);
    }

    @Override
    public MtRoomVO getDetail(Long id) {
        MtRoom room = mtRoomMapper.selectById(id);
        return room != null ? toVO(room) : null;
    }

    @Override
    public List<MtRoomVO> listAll() {
        List<MtRoom> list = mtRoomMapper.selectList(null);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

