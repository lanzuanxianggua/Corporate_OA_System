package cn.oa.task.service.impl;

import cn.oa.platform.core.exception.BusinessException;
import cn.oa.task.entity.TaskComment;
import cn.oa.task.mapper.TaskCommentMapper;
import cn.oa.task.service.TaskCommentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评论管理服务实现
 */
@Service
public class TaskCommentServiceImpl extends ServiceImpl<TaskCommentMapper, TaskComment> implements TaskCommentService {

    @Autowired
    private TaskCommentMapper commentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addComment(TaskComment comment) {
        // 如果replyToId不为空，检查父评论存在
        if (comment.getReplyToId() != null) {
            TaskComment parent = commentMapper.selectById(comment.getReplyToId());
            if (parent == null) {
                throw new BusinessException("回复的评论不存在");
            }
        }
        commentMapper.insert(comment);
    }

    @Override
    public List<TaskComment> getComments(Long taskId) {
        return commentMapper.selectList(new LambdaQueryWrapper<TaskComment>()
                .eq(TaskComment::getTaskId, taskId)
                .orderByAsc(TaskComment::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id, Long empId) {
        TaskComment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getEmpId().equals(empId)) {
            throw new BusinessException("只能删除自己的评论");
        }
        commentMapper.deleteById(id);
    }
}
