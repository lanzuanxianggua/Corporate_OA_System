package cn.oa.task.service;

import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.task.dto.TaskCommentCreateDTO;
import cn.oa.task.entity.TaskComment;
import cn.oa.task.mapper.TaskCommentMapper;
import cn.oa.task.vo.TaskCommentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 任务评论 Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskCommentService {

    private final TaskCommentMapper mapper;

    @Transactional
    public Long create(TaskCommentCreateDTO dto, Long empId) {
        if (dto.getItemId() == null) {
            throw new BizException(RCode.BAD_REQUEST, "itemId 不能为空");
        }
        TaskComment c = new TaskComment();
        c.setItemId(dto.getItemId());
        c.setContent(dto.getContent());
        c.setEmpId(empId);
        c.setParentCommentId(dto.getParentCommentId());
        mapper.insert(c);
        log.info("任务评论已创建: id={}, itemId={}", c.getId(), c.getItemId());
        return c.getId();
    }

    @Transactional
    public void delete(Long id, Long empId) {
        TaskComment c = mapper.selectById(id);
        if (c == null) throw new BizException(RCode.NOT_FOUND, "评论不存在");
        if (!c.getEmpId().equals(empId)) {
            throw new BizException(RCode.FORBIDDEN, "只能删除自己的评论");
        }
        mapper.deleteById(id);
        log.info("任务评论已删除: id={}", id);
    }

    public List<TaskCommentVO> listByItem(Long itemId) {
        List<TaskComment> list = mapper.findByItemIdOrderByTime(itemId);
        return list.stream().map(this::toVO).toList();
    }

    private TaskCommentVO toVO(TaskComment c) {
        TaskCommentVO vo = new TaskCommentVO();
        vo.setId(c.getId());
        vo.setItemId(c.getItemId());
        vo.setContent(c.getContent());
        vo.setEmpId(c.getEmpId());
        vo.setParentCommentId(c.getParentCommentId());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }
}
