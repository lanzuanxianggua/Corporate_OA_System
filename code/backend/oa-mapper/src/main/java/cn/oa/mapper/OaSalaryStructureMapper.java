package cn.oa.mapper;

import cn.oa.entity.OaSalaryStructure;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OaSalaryStructureMapper extends BaseMapper<OaSalaryStructure> {

    @Select("<script>" +
            "SELECT s.*, e.emp_name " +
            "FROM oa_salary_structure s " +
            "LEFT JOIN sys_employee e ON s.emp_id = e.id " +
            "WHERE s.del_flag = 0 " +
            "<if test='empId != null'>" +
            "  AND s.emp_id = #{empId} " +
            "</if>" +
            "<if test='searchKey != null and searchKey != \"\"'>" +
            "  AND e.emp_name LIKE CONCAT('%', #{searchKey}, '%') " +
            "</if>" +
            " ORDER BY s.create_time DESC" +
            "</script>")
    IPage<OaSalaryStructure> pageWithEmpInfo(Page<OaSalaryStructure> page,
                                              @Param("empId") Long empId,
                                              @Param("searchKey") String searchKey);
}
