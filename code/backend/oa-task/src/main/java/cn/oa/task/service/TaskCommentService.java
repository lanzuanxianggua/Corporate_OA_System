package cn.oa.task.service;

import cn.oa.task.entity.TaskComment;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface TaskCommentService extends IService<TaskComment> {

    /**
     * 添加评论
     */
    void addComment(TaskComment comment);

    /**
     * 查询任务的评论列表（按创建时间升序）
     */
    List<TaskComment> getComments(Long taskId);

    /**
     * 删除评论（逻辑删除）
     */
    void deleteComment(Long id, Long empId);
}
