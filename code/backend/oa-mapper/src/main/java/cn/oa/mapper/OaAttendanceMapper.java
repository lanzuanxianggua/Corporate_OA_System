package cn.oa.mapper;

import cn.oa.entity.OaAttendance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface OaAttendanceMapper extends BaseMapper<OaAttendance> {

    /**
     * 统计一批员工在日期范围内的出勤情况：总天数、正常天数
     */
    @Select("SELECT emp_id, " +
            "       COUNT(*) AS total, " +
            "       COUNT(CASE WHEN status = 0 THEN 1 END) AS normal " +
            "FROM oa_attendance " +
            "WHERE work_date BETWEEN #{startDate} AND #{endDate} " +
            "  AND del_flag = 0 " +
            "GROUP BY emp_id")
    List<Map<String, Object>> selectAttendanceStatsGroupByEmp(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 统计一批员工在日期范围内的迟到次数
     */
    @Select("SELECT emp_id, COUNT(*) AS late_count " +
            "FROM oa_attendance " +
            "WHERE work_date BETWEEN #{startDate} AND #{endDate} " +
            "  AND status = 1 " +
            "  AND del_flag = 0 " +
            "GROUP BY emp_id")
    List<Map<String, Object>> selectLateCountGroupByEmp(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
