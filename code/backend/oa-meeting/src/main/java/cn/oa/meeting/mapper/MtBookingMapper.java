package cn.oa.meeting.mapper;

import cn.oa.meeting.entity.MtBooking;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 会议室预约 Mapper.
 */
@Mapper
public interface MtBookingMapper extends BaseMapper<MtBooking> {

    /**
     * 按会议室和日期查询预约.
     */
    @Select("""
        <script>
        SELECT * FROM mt_bookings
        WHERE del_flag = '0'
          AND room_id = #{roomId}
          AND book_date = #{bookDate}
          AND status IN ('PENDING', 'APPROVED')
        </script>
        """)
    List<MtBooking> findByRoomAndDate(@Param("roomId") Long roomId, @Param("bookDate") LocalDate bookDate);

    /**
     * 查询预约详情(含会议室名称和预订人姓名).
     */
    @Select("""
        SELECT
          b.*,
          r.room_name,
          e.real_name AS book_emp_name
        FROM mt_bookings b
        LEFT JOIN mt_rooms r ON b.room_id = r.id AND r.del_flag = '0'
        LEFT JOIN sys_employee e ON b.emp_id = e.id AND e.del_flag = '0'
        WHERE b.id = #{id}
          AND b.del_flag = '0'
        """)
    Map<String, Object> selectDetailById(@Param("id") Long id);
}
