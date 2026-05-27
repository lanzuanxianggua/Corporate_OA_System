package cn.oa.mapper;

import cn.oa.entity.OaEmpArchive;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OaEmpArchiveMapper extends BaseMapper<OaEmpArchive> {

    @Select("<script>" +
            "SELECT a.*, e.emp_name, e.emp_code AS emp_no, d.dept_name, e.phone, e.email " +
            "FROM oa_emp_archive a " +
            "LEFT JOIN sys_employee e ON a.emp_id = e.id " +
            "LEFT JOIN sys_dept d ON e.dept_id = d.id " +
            "WHERE a.del_flag = 0 " +
            "<if test='searchKey != null and searchKey != \"\"'>" +
            "  AND e.emp_name LIKE CONCAT('%', #{searchKey}, '%') " +
            "</if>" +
            " ORDER BY a.create_time DESC" +
            "</script>")
    IPage<OaEmpArchive> pageWithEmpInfo(Page<OaEmpArchive> page,
                                         @Param("searchKey") String searchKey);

    @Select("SELECT a.*, e.emp_name, e.emp_code AS emp_no, d.dept_name, e.phone, e.email " +
            "FROM oa_emp_archive a " +
            "LEFT JOIN sys_employee e ON a.emp_id = e.id " +
            "LEFT JOIN sys_dept d ON e.dept_id = d.id " +
            "WHERE a.del_flag = 0 AND a.emp_id = #{empId}")
    OaEmpArchive getByEmpIdWithInfo(@Param("empId") Long empId);
}
