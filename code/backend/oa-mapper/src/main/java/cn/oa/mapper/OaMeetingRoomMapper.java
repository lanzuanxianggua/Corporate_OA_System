package cn.oa.mapper;

import cn.oa.entity.OaMeetingRoom;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OaMeetingRoomMapper extends BaseMapper<OaMeetingRoom> {

    @Select("SELECT id FROM oa_meeting_room WHERE id = #{roomId} FOR UPDATE")
    Long lockById(@Param("roomId") Long roomId);
}
