package cn.oa.knowledge.mapper;

import cn.oa.knowledge.entity.KmVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 版本历史 Mapper.
 */
@Mapper
public interface KmVersionMapper extends BaseMapper<KmVersion> {

    /**
     * 按条目查询版本历史 (降序).
     */
    @Select("""
        SELECT id, entry_id, version_no, title, summary, change_note,
               create_emp_id, create_by, create_time
        FROM km_versions
        WHERE del_flag = '0'
          AND entry_id = #{entryId}
        ORDER BY version_no DESC
        """)
    List<KmVersion> findByEntryIdOrderByVersion(@Param("entryId") Long entryId);
}
