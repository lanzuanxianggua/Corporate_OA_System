package cn.oa.workflow.service;

import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.engine.WfEngine;
import cn.oa.workflow.entity.WfInstance;
import cn.oa.workflow.entity.WfTask;
import cn.oa.workflow.mapper.WfInstanceMapper;
import cn.oa.workflow.mapper.WfTaskMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程实例服务.
 */
@Service
public class WfInstanceService {

    private final WfEngine engine;
    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;

    public WfInstanceService(WfEngine engine,
                             WfInstanceMapper instanceMapper,
                             WfTaskMapper taskMapper) {
        this.engine = engine;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
    }

    public Long start(String defKey, String businessKey, Long initiator) {
        return engine.startProcess(defKey, businessKey, initiator);
    }

    public WfInstance getById(Long id) {
        WfInstance instance = instanceMapper.selectById(id);
        if (instance == null) {
            throw new BizException(RCode.NOT_FOUND, "流程实例不存在: " + id);
        }
        return instance;
    }

    public List<WfTask> getTasks(Long instanceId) {
        return taskMapper.findByInstanceId(instanceId);
    }
}
