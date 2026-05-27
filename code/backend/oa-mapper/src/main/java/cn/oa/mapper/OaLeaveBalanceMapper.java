package cn.oa.mapper;

import cn.oa.entity.OaLeaveBalance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OaLeaveBalanceMapper extends BaseMapper<OaLeaveBalance> {

    @Select("<script>" +
            "SELECT b.*, e.emp_name, d.dept_name " +
            "FROM oa_leave_balance b " +
            "LEFT JOIN sys_employee e ON b.emp_id = e.id " +
            "LEFT JOIN sys_dept d ON e.dept_id = d.id " +
            "WHERE 1 = 1 " +
            "<if test='empId != null'>" +
            "  AND b.emp_id = #{empId} " +
            "</if>" +
            "<if test='year != null'>" +
            "  AND b.year = #{year} " +
            "</if>" +
            "<if test='searchKey != null and searchKey != \"\"'>" +
            "  AND e.emp_name LIKE CONCAT('%', #{searchKey}, '%') " +
            "</if>" +
            " ORDER BY b.create_time DESC" +
            "</script>")
    IPage<OaLeaveBalance> pageWithEmpInfo(Page<OaLeaveBalance> page,
                                           @Param("empId") Long empId,
                                           @Param("year") Integer year,
                                           @Param("searchKey") String searchKey);
}
