package cn.oa.knowledge.mapper;

import cn.oa.knowledge.entity.KmCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识分类 Mapper.
 */
@Mapper
public interface KmCategoryMapper extends BaseMapper<KmCategory> {

    /**
     * 查询所有分类 (含软删除判断, 按 sort_order 排序).
     */
    @Select("""
        SELECT id, category_name, parent_id, sort_order, description,
               create_by, create_time, update_by, update_time, version
        FROM km_categories
        WHERE del_flag = '0'
        ORDER BY sort_order ASC, id ASC
        """)
    List<KmCategory> findTree();
}
