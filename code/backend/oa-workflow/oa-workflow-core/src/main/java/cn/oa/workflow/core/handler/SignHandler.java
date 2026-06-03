package cn.oa.workflow.core.handler;

import cn.oa.workflow.core.engine.AggregationResult;
import cn.oa.workflow.mapper.WfNodeMapper;
import cn.oa.workflow.mapper.WfTaskMapper;
import cn.oa.workflow.model.constant.WorkflowConstants;
import cn.oa.workflow.model.entity.WfNode;
import cn.oa.workflow.model.entity.WfTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会签处理器
 * 实现 COUNTERSIGN/ORSIGN/PROPORTIONAL/VOTE 四种会签算法
 */
@Slf4j
@Component
public class SignHandler {

    @Autowired
    private WfTaskMapper taskMapper;

    @Autowired
    private WfNodeMapper nodeMapper;

    /**
     * 会签结果
     */
    public enum SignResult {
        /** 等待中 */
        WAITING,
        /** 通过 */
        APPROVED,
        /** 驳回 */
        REJECTED
    }

    /**
     * 评估会签结果
     *
     * @param parentTaskId 父任务ID
     * @param instanceId   实例ID（用于日志）
     * @return 会签结果
     */
    public AggregationResult evaluateSign(Long parentTaskId, Long instanceId) {
        List<WfTask> childTasks = taskMapper.selectByParentTaskId(parentTaskId);
        if (childTasks == null || childTasks.isEmpty()) {
            log.warn("No child tasks found for parentTaskId={}", parentTaskId);
            return AggregationResult.waiting();
        }

        // 获取节点的审批模式
        WfTask sampleTask = childTasks.get(0);
        WfNode node = nodeMapper.selectById(sampleTask.getNodeId());
        if (node == null) {
            log.error("Node not found for nodeId={}", sampleTask.getNodeId());
            return AggregationResult.waiting();
        }

        String mode = node.getApprovalMode();
        if (mode == null) {
            mode = WorkflowConstants.APPROVAL_MODE_COUNTERSIGN;
        }

        log.debug("Evaluating sign: parentTaskId={}, mode={}, totalTasks={}",
                parentTaskId, mode, childTasks.size());

        switch (mode) {
            case WorkflowConstants.APPROVAL_MODE_COUNTERSIGN:
                return evaluateCountersign(childTasks, node);
            case WorkflowConstants.APPROVAL_MODE_ORSIGN:
                return evaluateOrsign(childTasks);
            case WorkflowConstants.APPROVAL_MODE_PROPORTIONAL:
                return evaluateProportional(childTasks, node);
            case WorkflowConstants.APPROVAL_MODE_VOTE:
                return evaluateVote(childTasks);
            default:
                log.warn("Unknown approval mode: {}, defaulting to countersign", mode);
                return evaluateCountersign(childTasks, node);
        }
    }

    /**
     * 会签算法：所有人同意才通过
     * 规则：
     * - 任一人拒绝即驳回（支持一票否决）
     * - 全部同意才通过
     */
    private AggregationResult evaluateCountersign(List<WfTask> tasks, WfNode node) {
        long total = tasks.size();
        long approved = countByStatus(tasks, WorkflowConstants.TASK_STATUS_APPROVED);
        long rejected = countByStatus(tasks, WorkflowConstants.TASK_STATUS_REJECTED);
        long pending = countByStatus(tasks, WorkflowConstants.TASK_STATUS_PENDING);

        // 一票否决检查
        if (rejected > 0) {
            boolean vetoEnabled = node.getVetoEnabled() != null && node.getVetoEnabled() == 1;
            if (vetoEnabled || approved + pending < total) {
                cancelRemainingTasks(tasks);
                return AggregationResult.rejected("会签驳回：有人不同意");
            }
        }

        // 全部同意
        if (pending == 0 && rejected == 0) {
            return AggregationResult.approved();
        }

        return AggregationResult.waiting();
    }

    /**
     * 或签算法：任一人同意即通过
     * 规则：
     * - 任一人同意即通过
     * - 全部拒绝才驳回
     */
    private AggregationResult evaluateOrsign(List<WfTask> tasks) {
        long approved = countByStatus(tasks, WorkflowConstants.TASK_STATUS_APPROVED);
        long pending = countByStatus(tasks, WorkflowConstants.TASK_STATUS_PENDING);

        // 任一人同意
        if (approved > 0) {
            cancelRemainingTasks(tasks);
            return AggregationResult.approved();
        }

        // 全部拒绝
        if (pending == 0) {
            return AggregationResult.rejected("或签驳回：无人同意");
        }

        return AggregationResult.waiting();
    }

    /**
     * 比例算法：达到指定比例即通过
     * 规则：
     * - 同意人数占比达到阈值即通过
     * - 剩余未投人数不足以达成比例即驳回
     */
    private AggregationResult evaluateProportional(List<WfTask> tasks, WfNode node) {
        long total = tasks.size();
        long approved = countByStatus(tasks, WorkflowConstants.TASK_STATUS_APPROVED);
        long rejected = countByStatus(tasks, WorkflowConstants.TASK_STATUS_REJECTED);
        long pending = countByStatus(tasks, WorkflowConstants.TASK_STATUS_PENDING);

        // 获取通过比例，默认50%
        double passRatio = node.getPassRatio() != null ? node.getPassRatio() : 0.5;

        // 已达到比例
        double approvedRatio = (double) approved / total;
        if (approvedRatio >= passRatio) {
            cancelRemainingTasks(tasks);
            return AggregationResult.approved();
        }

        // 剩余未投人数不足以达成比例
        double maxPossibleRatio = (double) (approved + pending) / total;
        if (maxPossibleRatio < passRatio) {
            cancelRemainingTasks(tasks);
            return AggregationResult.rejected(
                    String.format("比例不足：当前%.1f%%，需%.1f%%", approvedRatio * 100, passRatio * 100));
        }

        return AggregationResult.waiting();
    }

    /**
     * 投票算法：少数服从多数
     * 规则：
     * - 所有投票完成后统计
     * - 同意票数 > 拒绝票数即通过
     */
    private AggregationResult evaluateVote(List<WfTask> tasks) {
        long approved = countByStatus(tasks, WorkflowConstants.TASK_STATUS_APPROVED);
        long rejected = countByStatus(tasks, WorkflowConstants.TASK_STATUS_REJECTED);
        long pending = countByStatus(tasks, WorkflowConstants.TASK_STATUS_PENDING);

        // 所有投票完成
        if (pending == 0) {
            if (approved > rejected) {
                return AggregationResult.approved();
            } else {
                return AggregationResult.rejected(
                        String.format("投票不通过：同意%d票，反对%d票", approved, rejected));
            }
        }

        return AggregationResult.waiting();
    }

    /**
     * 统计指定状态的任务数量
     */
    private long countByStatus(List<WfTask> tasks, String status) {
        return tasks.stream()
                .filter(t -> status.equals(t.getStatus()))
                .count();
    }

    /**
     * 取消剩余待处理任务
     */
    private void cancelRemainingTasks(List<WfTask> tasks) {
        for (WfTask task : tasks) {
            if (WorkflowConstants.TASK_STATUS_PENDING.equals(task.getStatus())) {
                task.setStatus(WorkflowConstants.TASK_STATUS_CANCELED);
                task.setEndTime(LocalDateTime.now());
                taskMapper.updateById(task);
                log.info("Canceled task {} due to sign result finalized", task.getId());
            }
        }
    }

    /**
     * 判断任务是否为会签任务
     */
    public boolean isCountersignTask(WfTask task) {
        if (task == null) return false;
        String taskType = task.getTaskType();
        return WorkflowConstants.TASK_TYPE_COUNTERSIGN.equals(taskType)
                || WorkflowConstants.APPROVAL_MODE_COUNTERSIGN.equals(taskType)
                || WorkflowConstants.APPROVAL_MODE_ORSIGN.equals(taskType)
                || WorkflowConstants.APPROVAL_MODE_PROPORTIONAL.equals(taskType)
                || WorkflowConstants.APPROVAL_MODE_VOTE.equals(taskType);
    }

    /**
     * 获取会签进度信息
     */
    public SignProgress getProgress(Long parentTaskId) {
        List<WfTask> tasks = taskMapper.selectByParentTaskId(parentTaskId);
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }

        long total = tasks.size();
        long approved = countByStatus(tasks, WorkflowConstants.TASK_STATUS_APPROVED);
        long rejected = countByStatus(tasks, WorkflowConstants.TASK_STATUS_REJECTED);
        long pending = countByStatus(tasks, WorkflowConstants.TASK_STATUS_PENDING);

        return new SignProgress(total, approved, rejected, pending);
    }

    /**
     * 会签进度信息
     */
    public record SignProgress(long total, long approved, long rejected, long pending) {
        public double getApprovedRatio() {
            return total > 0 ? (double) approved / total : 0;
        }

        public boolean isComplete() {
            return pending == 0;
        }
    }
}
