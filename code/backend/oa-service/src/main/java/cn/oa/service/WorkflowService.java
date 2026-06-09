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

    IPage<WfTask> myHandledTasks(Long assigneeId, int pageNum, int pageSize);

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

    /**
     * V1010: public hook for the timeout-escalation scheduler. Resolves an
     * assignee from a node-level escalateTo config without going through the
     * graph materialization path. Currently delegates to the same machinery
     * as startProcess routing.
     */
    Long resolveAssigneeForEscalation(String assigneeType, String assigneeValue, Long currentAssigneeId);

    /**
     * V1010: parse + validate a graph-format workflow definition. Returns the
     * parsed graph (with errors populated when invalid). The graph object is
     * useful both for validation responses and for downstream operations.
     */
    cn.oa.service.impl.WorkflowServiceImpl.WorkflowGraph validateDefinition(String nodeConfig);

    /**
     * V1010: parse either legacy flat-array nodeConfig or graph-format
     * nodeConfig using the same parser as runtime routing.
     */
    cn.oa.service.impl.WorkflowServiceImpl.WorkflowGraph parseNodeConfig(String nodeConfig);

    /**
     * V1010: preview the routing path a real business submission would take,
     * without actually starting a process. Returns the materialized flat-array
     * node list (same shape as legacy definitions) so the frontend can render
     * the path highlight.
     */
    java.util.List<cn.hutool.json.JSONObject> previewPath(String businessType, Long businessId, Long initiatorId);
}
