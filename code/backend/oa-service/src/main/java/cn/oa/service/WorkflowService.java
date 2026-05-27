package cn.oa.service;

import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.WfProcessDefinition;
import cn.oa.entity.WfProcessInstance;
import cn.oa.entity.WfTask;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface WorkflowService extends IService<WfProcessDefinition> {

    WfProcessInstance startProcess(String businessType, Long businessId, Long initiatorId);

    WfProcessInstance startProcess(String businessType, Long businessId, Long initiatorId, Map<String, Object> conditionContext);

    WfTask getCurrentTask(Long instanceId);

    void handleTask(Long taskId, Long handlerId, Integer status, String remark);

    IPage<WfTask> myPendingTasks(Long assigneeId, int pageNum, int pageSize);

    WfProcessInstance getByBusiness(String businessType, Long businessId);

    List<WfProcessDefinition> listDefinitions();

    void saveDefinition(WfProcessDefinition definition);

    WfTask findPendingTask(String businessType, Long businessId, Long assigneeId);

    List<WfTask> getApprovalHistory(String businessType, Long businessId);

    void withdrawProcess(String businessType, Long businessId, Long initiatorId);

    void transferTask(Long taskId, Long fromAssigneeId, Long toAssigneeId, String reason);

    void urgeTask(Long instanceId, Long initiatorId);

    List<OaApprovalRecord> getApprovalChain(String businessType, Long businessId);

    void returnTask(Long taskId, Long handlerId, String returnTarget, String remark);
}
