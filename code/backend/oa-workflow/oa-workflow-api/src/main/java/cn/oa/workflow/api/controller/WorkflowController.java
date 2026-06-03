package cn.oa.workflow.api.controller;

import cn.oa.workflow.core.engine.IWorkflowEngine;
import cn.oa.workflow.model.constant.WorkflowConstants;
import cn.oa.workflow.model.dto.*;
import cn.oa.workflow.model.entity.*;
import cn.oa.workflow.mapper.*;
import cn.oa.platform.core.base.R;
import cn.oa.platform.core.base.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工作流 REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow")
@Tag(name = "工作流引擎 V2")
public class WorkflowController {

    @Autowired private IWorkflowEngine workflowEngine;
    @Autowired private WfDefinitionMapper definitionMapper;
    @Autowired private WfNodeMapper nodeMapper;
    @Autowired private WfInstanceMapper instanceMapper;
    @Autowired private WfTaskMapper taskMapper;
    @Autowired private WfDelegationMapper delegationMapper;
    @Autowired private WfRecordMapper recordMapper;

    // ==================== 流程定义 ====================

    @PostMapping("/definition")
    @Operation(summary = "创建/更新流程定义")
    public R<WfDefinition> saveDefinition(@RequestBody @Valid WfDefinition definition) {
        if (definition.getId() != null) {
            definitionMapper.updateById(definition);
        } else {
            definition.setVersion(1);
            definition.setStatus(WorkflowConstants.DEF_STATUS_DRAFT);
            definitionMapper.insert(definition);
        }
        return R.ok(definition);
    }

    @PostMapping("/definition/publish/{id}")
    @Operation(summary = "发布流程定义")
    public R<Void> publishDefinition(@PathVariable Long id) {
        WfDefinition def = definitionMapper.selectById(id);
        if (def == null) return R.fail("流程定义不存在");
        // 旧版本禁用
        List<WfDefinition> oldVersions = definitionMapper.selectList(
                new LambdaQueryWrapper<WfDefinition>()
                        .eq(WfDefinition::getCode, def.getCode())
                        .eq(WfDefinition::getStatus, WorkflowConstants.DEF_STATUS_PUBLISHED));
        for (WfDefinition old : oldVersions) {
            old.setStatus(WorkflowConstants.DEF_STATUS_DISABLED);
            definitionMapper.updateById(old);
        }
        def.setVersion(def.getVersion() + 1);
        def.setStatus(WorkflowConstants.DEF_STATUS_PUBLISHED);
        definitionMapper.updateById(def);
        return R.ok();
    }

    @GetMapping("/definition/list")
    @Operation(summary = "查询所有流程定义")
    public R<List<WfDefinition>> listDefinitions() {
        return R.ok(definitionMapper.selectList(
                new LambdaQueryWrapper<WfDefinition>().orderByDesc(WfDefinition::getVersion)));
    }

    @GetMapping("/definition/{id}/nodes")
    @Operation(summary = "查询流程节点")
    public R<List<WfNode>> listNodes(@PathVariable Long id) {
        return R.ok(nodeMapper.selectByDefId(id));
    }

    @PostMapping("/definition/{id}/nodes")
    @Operation(summary = "保存流程节点")
    public R<Void> saveNodes(@PathVariable Long id, @RequestBody List<WfNode> nodes) {
        // 删除旧节点
        nodeMapper.delete(new LambdaQueryWrapper<WfNode>().eq(WfNode::getDefId, id));
        // 插入新节点
        for (WfNode node : nodes) {
            node.setDefId(id);
            nodeMapper.insert(node);
        }
        return R.ok();
    }

    // ==================== 流程操作 ====================

    @PostMapping("/process/start")
    @Operation(summary = "启动流程")
    public R<WfInstance> startProcess(@RequestBody @Valid StartProcessDTO dto,
                                       @RequestAttribute Long currentEmpId) {
        WfInstance instance = workflowEngine.startProcess(
                dto.getBusinessType(), dto.getBusinessId(), currentEmpId,
                dto.getConditionContext(), dto.getFormData());
        return R.ok(instance);
    }

    @PostMapping("/process/suspend/{instanceId}")
    @Operation(summary = "挂起流程")
    public R<Void> suspendProcess(@PathVariable Long instanceId, @RequestParam String reason) {
        workflowEngine.suspendInstance(instanceId, reason);
        return R.ok();
    }

    @PostMapping("/process/resume/{instanceId}")
    @Operation(summary = "恢复流程")
    public R<Void> resumeProcess(@PathVariable Long instanceId) {
        workflowEngine.resumeInstance(instanceId);
        return R.ok();
    }

    @PostMapping("/process/abort/{instanceId}")
    @Operation(summary = "终止流程")
    public R<Void> abortProcess(@PathVariable Long instanceId, @RequestParam String reason) {
        workflowEngine.abortInstance(instanceId, reason);
        return R.ok();
    }

    @PostMapping("/process/withdraw")
    @Operation(summary = "撤回流程")
    public R<Void> withdrawProcess(@RequestParam String businessType,
                                    @RequestParam Long businessId,
                                    @RequestAttribute Long currentEmpId) {
        WfInstance instance = instanceMapper.selectByBusiness(businessType, businessId);
        if (instance != null) {
            workflowEngine.withdrawInstance(instance.getId(), currentEmpId);
        }
        return R.ok();
    }

    // ==================== 任务操作 ====================

    @GetMapping("/task/pending")
    @Operation(summary = "我的待办")
    public R<PageResult<WfTask>> pendingTasks(@RequestParam int pageNum,
                                               @RequestParam int pageSize,
                                               @RequestAttribute Long currentEmpId) {
        IPage<WfTask> page = taskMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<WfTask>()
                        .eq(WfTask::getAssigneeId, currentEmpId)
                        .eq(WfTask::getStatus, WorkflowConstants.TASK_STATUS_PENDING)
                        .orderByDesc(WfTask::getCreatedAt));
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/task/handled")
    @Operation(summary = "我的已办")
    public R<PageResult<WfTask>> handledTasks(@RequestParam int pageNum,
                                               @RequestParam int pageSize,
                                               @RequestAttribute Long currentEmpId) {
        IPage<WfTask> page = taskMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<WfTask>()
                        .eq(WfTask::getAssigneeId, currentEmpId)
                        .ne(WfTask::getStatus, WorkflowConstants.TASK_STATUS_PENDING)
                        .orderByDesc(WfTask::getEndTime));
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/task/handle")
    @Operation(summary = "处理任务")
    public R<Void> handleTask(@RequestBody @Valid HandleTaskDTO dto,
                               @RequestAttribute Long currentEmpId) {
        if ("APPROVED".equals(dto.getResult())) {
            workflowEngine.approveTask(dto.getTaskId(), dto);
        } else if ("REJECTED".equals(dto.getResult())) {
            workflowEngine.rejectTask(dto.getTaskId(), dto);
        }
        return R.ok();
    }

    @PostMapping("/task/transfer")
    @Operation(summary = "转办任务")
    public R<Void> transferTask(@RequestBody @Valid TransferTaskDTO dto,
                                 @RequestAttribute Long currentEmpId) {
        workflowEngine.transferTask(dto.getTaskId(), dto);
        return R.ok();
    }

    @PostMapping("/task/add-sign")
    @Operation(summary = "加签")
    public R<Void> addSign(@RequestParam Long taskId,
                            @RequestParam Long addSignerId,
                            @RequestParam(defaultValue = "true") boolean isFront,
                            @RequestParam(required = false) String reason,
                            @RequestAttribute Long currentEmpId) {
        // TODO: 加签功能需要在 IWorkflowEngine 中添加接口
        log.info("Add sign requested: taskId={}, addSignerId={}, isFront={}", taskId, addSignerId, isFront);
        return R.ok();
    }

    @PostMapping("/task/urge/{instanceId}")
    @Operation(summary = "催办")
    public R<Void> urgeTask(@PathVariable Long instanceId,
                             @RequestAttribute Long currentEmpId) {
        workflowEngine.urgeTask(instanceId, currentEmpId);
        return R.ok();
    }

    // ==================== 查询 ====================

    @GetMapping("/instance/by-business")
    @Operation(summary = "根据业务查询流程实例")
    public R<WfInstance> getByBusiness(@RequestParam String businessType,
                                        @RequestParam Long businessId) {
        return R.ok(instanceMapper.selectByBusiness(businessType, businessId));
    }

    @GetMapping("/records/{instanceId}")
    @Operation(summary = "查询流转记录")
    public R<List<WfRecord>> getRecords(@PathVariable Long instanceId) {
        return R.ok(workflowEngine.getRecords(instanceId));
    }

    @GetMapping("/history/{instanceId}")
    @Operation(summary = "查询审批历史")
    public R<List<WfTask>> getHistory(@PathVariable Long instanceId) {
        return R.ok(workflowEngine.getApprovalHistory(instanceId));
    }

    // ==================== 委托 ====================

    @PostMapping("/delegation")
    @Operation(summary = "设置审批委托")
    public R<Void> setDelegation(@RequestBody @Valid WfDelegation delegation,
                                  @RequestAttribute Long currentEmpId) {
        delegation.setDelegatorId(currentEmpId);
        delegation.setStatus("ACTIVE");
        delegationMapper.insert(delegation);
        return R.ok();
    }

    @GetMapping("/delegation/my")
    @Operation(summary = "我的委托")
    public R<List<WfDelegation>> myDelegations(@RequestAttribute Long currentEmpId) {
        return R.ok(delegationMapper.selectList(
                new LambdaQueryWrapper<WfDelegation>()
                        .eq(WfDelegation::getDelegatorId, currentEmpId)));
    }

    @PostMapping("/delegation/cancel/{id}")
    @Operation(summary = "取消委托")
    public R<Void> cancelDelegation(@PathVariable Long id) {
        WfDelegation d = delegationMapper.selectById(id);
        if (d != null) {
            d.setStatus("CANCELED");
            delegationMapper.updateById(d);
        }
        return R.ok();
    }
}