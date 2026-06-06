package cn.oa.knowledge.mapper;

import cn.oa.knowledge.entity.KmEntry;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识条目 Mapper.
 */
@Mapper
public interface KmEntryMapper extends BaseMapper<KmEntry> {

    /**
     * 按分类查询 (不返回 content 大字段, 仅列表视图).
     */
    @Select("""
        SELECT id, title, summary, category_id, tags, status,
               view_count, create_emp_id, create_by, create_time, update_by, update_time
        FROM km_entries
        WHERE del_flag = '0'
          AND category_id = #{categoryId}
        ORDER BY create_time DESC
        """)
    List<KmEntry> findByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 全文搜索标题+摘要 (限制返回条数).
     */
    @Select("""
        SELECT id, title, summary, category_id, tags, status,
               view_count, create_emp_id, create_by, create_time, update_by, update_time
        FROM km_entries
        WHERE del_flag = '0'
          AND (title LIKE CONCAT('%', #{keyword}, '%')
               OR summary LIKE CONCAT('%', #{keyword}, '%'))
        ORDER BY view_count DESC, create_time DESC
        LIMIT #{limit}
        """)
    List<KmEntry> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
}
