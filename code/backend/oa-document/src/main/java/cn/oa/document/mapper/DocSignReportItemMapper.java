package cn.oa.document.mapper;

import cn.oa.document.entity.DocSignReportItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 签报审批记录明细 Mapper.
 */
@Mapper
public interface DocSignReportItemMapper extends BaseMapper<DocSignReportItem> {

    /**
     * 按签报 ID 查询审批记录 (按审批顺序升序).
     */
    @Select("SELECT * FROM doc_sign_report_items WHERE report_id = #{reportId} AND del_flag = '0' ORDER BY approve_order ASC")
    List<DocSignReportItem> findByReportIdOrderByOrder(@Param("reportId") Long reportId);
}
