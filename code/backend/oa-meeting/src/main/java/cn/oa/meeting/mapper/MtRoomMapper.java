package cn.oa.meeting.mapper;

import cn.oa.meeting.entity.MtRoom;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 会议室 Mapper.
 */
@Mapper
public interface MtRoomMapper extends BaseMapper<MtRoom> {

    /**
     * 查询会议室详情(含今日预约数).
     */
    @Select("""
        SELECT
          r.*,
          (SELECT COUNT(*) FROM mt_bookings b
            WHERE b.room_id = r.id
              AND b.del_flag = '0'
              AND b.book_date = CURDATE()
              AND b.status IN ('PENDING', 'APPROVED')) AS today_bookings
        FROM mt_rooms r
        WHERE r.id = #{id}
          AND r.del_flag = '0'
        """)
    Map<String, Object> selectDetailById(@Param("id") Long id);
}
