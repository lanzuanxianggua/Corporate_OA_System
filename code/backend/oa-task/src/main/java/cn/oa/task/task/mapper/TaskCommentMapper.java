package cn.oa.task.mapper;

import cn.oa.task.entity.TaskComment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 任务评论 Mapper.
 */
@Mapper
public interface TaskCommentMapper extends BaseMapper<TaskComment> {

    /**
     * 按任务查询评论列表, 按创建时间升序.
     */
    @Select("SELECT * FROM task_comments WHERE del_flag = '0' AND item_id = #{itemId} ORDER BY create_time ASC")
    List<TaskComment> findByItemIdOrderByTime(@Param("itemId") Long itemId);
}
