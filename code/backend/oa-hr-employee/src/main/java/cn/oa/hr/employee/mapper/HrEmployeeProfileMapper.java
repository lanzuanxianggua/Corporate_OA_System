package cn.oa.hr.employee.mapper;

import cn.oa.hr.employee.entity.HrEmployeeProfile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 员工档案 Mapper.
 */
@Mapper
public interface HrEmployeeProfileMapper extends BaseMapper<HrEmployeeProfile> {

    /**
     * 关联 sys_employee + sys_dept 查列表.
     */
    List<Map<String, Object>> findAllWithJoins(@Param("limit") int limit);

    /**
     * 关联详情.
     */
    Map<String, Object> findDetail(@Param("id") Long id);

    /**
     * 分页关联查询 (支持 keyword / status / contractType 筛选).
     */
    Page<Map<String, Object>> findPageWithJoins(
            Page<Map<String, Object>> page,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("contractType") String contractType
    );
}
