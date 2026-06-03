package cn.oa.workflow.core.engine;

import cn.oa.workflow.model.dto.HandleTaskDTO;
import cn.oa.workflow.model.dto.StartProcessDTO;
import cn.oa.workflow.model.dto.TransferTaskDTO;
import cn.oa.workflow.model.entity.WfInstance;
import cn.oa.workflow.model.entity.WfRecord;
import cn.oa.workflow.model.entity.WfTask;

import java.util.List;
import java.util.Map;

/**
 * 工作流引擎接口
 * 定义工作流核心操作
 */
public interface IWorkflowEngine {

    /**
     * 启动工作流
     *
     * @param dto 启动参数
     * @return 流程实例ID
     */
    Long startWorkflow(StartProcessDTO dto);

    /**
     * 启动工作流（完整参数）
     *
     * @param businessType      业务类型
     * @param businessId        业务ID
     * @param starterId         发起人ID
     * @param conditionContext  条件上下文
     * @param formData          表单数据
     * @return 流程实例
     */
    WfInstance startProcess(String businessType, Long businessId, Long starterId,
                            Map<String, Object> conditionContext, Map<String, Object> formData);

    /**
     * 审批通过任务
     *
     * @param taskId 任务ID
     * @param dto    处理参数
     */
    void approveTask(Long taskId, HandleTaskDTO dto);

    /**
     * 驳回任务
     *
     * @param taskId 任务ID
     * @param dto    处理参数
     */
    void rejectTask(Long taskId, HandleTaskDTO dto);

    /**
     * 转办任务
     *
     * @param taskId 任务ID
     * @param dto    转办参数
     */
    void transferTask(Long taskId, TransferTaskDTO dto);

    /**
     * 撤回流程
     *
     * @param instanceId 实例ID
     * @param initiatorId 发起人ID
     */
    void withdrawInstance(Long instanceId, Long initiatorId);

    /**
     * 挂起流程
     *
     * @param instanceId 实例ID
     * @param reason     挂起原因
     */
    void suspendInstance(Long instanceId, String reason);

    /**
     * 恢复流程
     *
     * @param instanceId 实例ID
     */
    void resumeInstance(Long instanceId);

    /**
     * 终止流程
     *
     * @param instanceId 实例ID
     * @param reason     终止原因
     */
    void abortInstance(Long instanceId, String reason);

    /**
     * 催办任务
     *
     * @param instanceId 实例ID
     * @param initiatorId 发起人ID
     */
    void urgeTask(Long instanceId, Long initiatorId);

    /**
     * 获取审批记录
     *
     * @param instanceId 实例ID
     * @return 审批记录列表
     */
    List<WfRecord> getRecords(Long instanceId);

    /**
     * 获取审批历史
     *
     * @param instanceId 实例ID
     * @return 任务历史列表
     */
    List<WfTask> getApprovalHistory(Long instanceId);
}
