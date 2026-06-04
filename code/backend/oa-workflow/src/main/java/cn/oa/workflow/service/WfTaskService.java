package cn.oa.workflow.service;

import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.engine.WfEngine;
import cn.oa.workflow.entity.WfTask;
import cn.oa.workflow.mapper.WfTaskMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 任务服务.
 */
@Service
public class WfTaskService {

    private final WfEngine engine;
    private final WfTaskMapper taskMapper;

    public WfTaskService(WfEngine engine, WfTaskMapper taskMapper) {
        this.engine = engine;
        this.taskMapper = taskMapper;
    }

    public List<WfTask> myPending(Long empId) {
        return taskMapper.findPendingByAssignee(empId);
    }

    public WfTask getById(Long id) {
        WfTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException(RCode.NOT_FOUND, "任务不存在: " + id);
        }
        return task;
    }

    public void approve(Long taskId, Long empId, String action, String comment) {
        engine.approve(taskId, empId, action, comment);
    }
}
