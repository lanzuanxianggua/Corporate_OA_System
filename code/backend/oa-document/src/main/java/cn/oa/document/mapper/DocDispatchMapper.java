package cn.oa.document.mapper;

import cn.oa.document.entity.DocDispatch;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 发文 Mapper.
 */
@Mapper
public interface DocDispatchMapper extends BaseMapper<DocDispatch> {

    /**
     * 关联 sys_employee + sys_dept 分页查询.
     */
    Page<Map<String, Object>> findPageWithJoins(
            Page<Map<String, Object>> page,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("deptId") Long deptId
    );

    /**
     * 发文详情 (关联员工/部门名).
     */
    Map<String, Object> findDetail(@Param("id") Long id);
}
