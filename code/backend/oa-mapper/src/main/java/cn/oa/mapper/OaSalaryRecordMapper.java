package cn.oa.mapper;

import cn.oa.entity.OaSalaryRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OaSalaryRecordMapper extends BaseMapper<OaSalaryRecord> {

    @Select("<script>" +
            "SELECT r.*, e.emp_name " +
            "FROM oa_salary_record r " +
            "LEFT JOIN sys_employee e ON r.emp_id = e.id " +
            "WHERE 1 = 1 " +
            "<if test='empId != null'>" +
            "  AND r.emp_id = #{empId} " +
            "</if>" +
            "<if test='salaryMonth != null and salaryMonth != \"\"'>" +
            "  AND r.salary_month = #{salaryMonth} " +
            "</if>" +
            "<if test='searchKey != null and searchKey != \"\"'>" +
            "  AND e.emp_name LIKE CONCAT('%', #{searchKey}, '%') " +
            "</if>" +
            " ORDER BY r.create_time DESC" +
            "</script>")
    IPage<OaSalaryRecord> pageWithEmpInfo(Page<OaSalaryRecord> page,
                                           @Param("empId") Long empId,
                                           @Param("salaryMonth") String salaryMonth,
                                           @Param("searchKey") String searchKey);
}
