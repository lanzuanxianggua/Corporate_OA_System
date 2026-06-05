package cn.oa.document.mapper;

import cn.oa.document.entity.DocSignReport;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 签报 Mapper.
 */
@Mapper
public interface DocSignReportMapper extends BaseMapper<DocSignReport> {

    /**
     * 关联 sys_employee + sys_dept 分页查询.
     */
    Page<Map<String, Object>> findPageWithJoins(
            Page<Map<String, Object>> page,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("empId") Long empId
    );

    /**
     * 签报详情.
     */
    Map<String, Object> findDetail(@Param("id") Long id);
}
