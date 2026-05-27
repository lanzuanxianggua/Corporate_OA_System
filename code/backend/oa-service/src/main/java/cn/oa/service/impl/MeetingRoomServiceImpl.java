package cn.oa.service.impl;

import cn.oa.entity.OaMeetingRoom;
import cn.oa.mapper.OaMeetingRoomMapper;
import cn.oa.service.MeetingRoomService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class MeetingRoomServiceImpl extends ServiceImpl<OaMeetingRoomMapper, OaMeetingRoom> implements MeetingRoomService {
}
