package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.annotation.RequireRole;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.PageParamUtil;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.WfCcRecord;
import cn.oa.entity.WfDelegation;
import cn.oa.entity.WfProcessDefinition;
import cn.oa.entity.WfProcessInstance;
import cn.oa.entity.WfTask;
import cn.oa.entity.dto.ActivateDefinitionDTO;
import cn.oa.entity.dto.BusinessRefDTO;
import cn.oa.entity.dto.HandleTaskDTO;
import cn.oa.entity.dto.ReturnTaskDTO;
import cn.oa.entity.dto.TransferTaskDTO;
import cn.oa.service.DelegationService;
import cn.oa.service.WorkflowService;
import cn.oa.mapper.WfCcRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/workflow")
@Tag(name = "工作流管理")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WfCcRecordMapper ccRecordMapper;

    @Autowired
    private DelegationService delegationService;

    @PostMapping("/definition")
    @RequireAdmin
    @Operation(summary = "创建/更新流程定义")
    @OperationLog(module = "工作流管理", operation = "保存流程定义")
    public R<Void> saveDefinition(@RequestBody @Valid WfProcessDefinition definition) {
        workflowService.saveDefinition(definition);
        log.info("Workflow definition saved: processType={}", definition.getProcessType());
        return R.ok();
    }

    @GetMapping("/definition/list")
    @Operation(summary = "查询所有流程定义")
    public R<List<WfProcessDefinition>> listDefinitions() {
        return R.ok(workflowService.listDefinitions());
    }

    @GetMapping("/task/pending")
    @Operation(summary = "我的待办任务")
    public R<PageResult<WfTask>> pendingTasks(@RequestParam int pageNum,
                                               @RequestParam int pageSize,
                                               HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        IPage<WfTask> page = workflowService.myPendingTasks(empId, PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize));
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/task/handled")
    @Operation(summary = "我的已办任务")
    public R<PageResult<WfTask>> handledTasks(@RequestParam int pageNum,
                                                @RequestParam int pageSize,
                                                HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        IPage<WfTask> page = workflowService.myHandledTasks(empId, PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize));
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/task/handle")
    @Operation(summary = "处理任务（审批/驳回）")
    @OperationLog(module = "工作流管理", operation = "处理任务")
    public R<Void> handleTask(@RequestBody @Valid HandleTaskDTO dto, HttpServletRequest request) {
        Long handlerId = WebUtil.getEmpId(request);
        workflowService.handleTask(dto.getTaskId(), handlerId, dto.getStatus(), dto.getRemark());
        log.info("Workflow task handled: taskId={}, status={}, handlerId={}", dto.getTaskId(), dto.getStatus(), handlerId);
        return R.ok();
    }

    @PostMapping("/definition/activate")
    @RequireRole({"ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"})
    @Operation(summary = "激活/停用流程定义")
    @OperationLog(module = "工作流管理", operation = "激活/停用流程定义")
    public R<Void> activateDefinition(@RequestBody @Valid ActivateDefinitionDTO dto) {
        WfProcessDefinition def = workflowService.getById(dto.getId());
        if (def == null) return R.fail("流程定义不存在");
        def.setStatus("0".equals(def.getStatus()) ? "1" : "0");
        workflowService.updateById(def);
        log.info("Workflow definition activated/deactivated: id={}, status={}", dto.getId(), def.getStatus());
        return R.ok();
    }

    @GetMapping("/history")
    @Operation(summary = "查询审批历史")
    public R<List<WfTask>> approvalHistory(@RequestParam String businessType,
                                            @RequestParam Long businessId) {
        return R.ok(workflowService.getApprovalHistory(businessType, businessId));
    }

    @GetMapping("/approval-chain")
    @Operation(summary = "查询审批意见链")
    public R<List<OaApprovalRecord>> approvalChain(@RequestParam String businessType,
                                                    @RequestParam Long businessId) {
        return R.ok(workflowService.getApprovalChain(businessType, businessId));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "撤回申请")
    @OperationLog(module = "工作流管理", operation = "撤回申请")
    public R<Void> withdraw(@RequestBody @Valid BusinessRefDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        workflowService.withdrawProcess(dto.getBusinessType(), dto.getBusinessId(), empId);
        log.info("Workflow withdrawn: businessType={}, businessId={}, empId={}", dto.getBusinessType(), dto.getBusinessId(), empId);
        return R.ok();
    }

    @GetMapping("/task/find")
    @Operation(summary = "查找待处理任务")
    public R<WfTask> findTask(@RequestParam String businessType, @RequestParam Long businessId, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        WfTask task = workflowService.findPendingTask(businessType, businessId, empId);
        return R.ok(task);
    }

    @PostMapping("/task/transfer")
    @Operation(summary = "转办任务")
    @OperationLog(module = "工作流管理", operation = "转办任务")
    public R<Void> transferTask(@RequestBody @Valid TransferTaskDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        workflowService.transferTask(dto.getTaskId(), empId, dto.getToAssigneeId(), dto.getReason());
        log.info("Workflow task transferred: taskId={}, from={}, to={}", dto.getTaskId(), empId, dto.getToAssigneeId());
        return R.ok();
    }

    @PostMapping("/task/return")
    @Operation(summary = "退回任务到指定节点")
    @OperationLog(module = "工作流管理", operation = "退回任务")
    public R<Void> returnTask(@RequestBody @Valid ReturnTaskDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        workflowService.returnTask(dto.getTaskId(), empId, dto.getReturnTarget(), dto.getRemark());
        log.info("Workflow task returned: taskId={}, target={}", dto.getTaskId(), dto.getReturnTarget());
        return R.ok();
    }

    @PostMapping("/task/urge")
    @RequireRole({"DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"})
    @Operation(summary = "催办")
    @OperationLog(module = "工作流管理", operation = "催办")
    public R<Void> urgeTask(@RequestBody @Valid BusinessRefDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        WfProcessInstance instance = workflowService.getByBusiness(dto.getBusinessType(), dto.getBusinessId());
        if (instance == null) {
            return R.fail("流程实例不存在");
        }
        workflowService.urgeTask(instance.getId(), empId);
        return R.ok();
    }

    @GetMapping("/cc/my")
    @Operation(summary = "我的抄送")
    public R<PageResult<WfCcRecord>> myCcRecords(@RequestParam int pageNum, @RequestParam int pageSize, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        Page<WfCcRecord> page = new Page<>(PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize));
        LambdaQueryWrapper<WfCcRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfCcRecord::getCcEmpId, empId).orderByDesc(WfCcRecord::getCreateTime);
        IPage<WfCcRecord> result = ccRecordMapper.selectPage(page, wrapper);
        return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
    }

    @PostMapping("/cc/read/{id}")
    @Operation(summary = "标记抄送已读")
    public R<Void> readCc(@PathVariable Long id) {
        WfCcRecord record = ccRecordMapper.selectById(id);
        if (record != null) {
            record.setStatus("1");
            ccRecordMapper.updateById(record);
        }
        return R.ok();
    }

    @PostMapping("/delegation/set")
    @Operation(summary = "设置审批委托")
    @OperationLog(module = "工作流管理", operation = "设置审批委托")
    public R<Void> setDelegation(@RequestBody @Valid WfDelegation delegation, HttpServletRequest request) {
        Long delegatorId = WebUtil.getEmpId(request);
        delegation.setDelegatorId(delegatorId);
        delegationService.setDelegation(delegation);
        log.info("Delegation set: delegatorId={}, delegateToId={}", delegatorId, delegation.getDelegateToId());
        return R.ok();
    }

    @GetMapping("/delegation/my")
    @Operation(summary = "我的审批委托")
    public R<List<WfDelegation>> myDelegations(HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        return R.ok(delegationService.getMyDelegations(empId));
    }

    @PostMapping("/delegation/cancel/{id}")
    @Operation(summary = "取消审批委托")
    @OperationLog(module = "工作流管理", operation = "取消审批委托")
    public R<Void> cancelDelegation(@PathVariable Long id, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        delegationService.cancelDelegation(id, empId);
        log.info("Delegation cancelled: id={}, empId={}", id, empId);
        return R.ok();
    }

    // ====== V1010: graph-format definition validation + path preview ======

    @PostMapping("/definition/validate")
    @RequireAdmin
    @Operation(summary = "校验流程定义 (V1010 图格式)")
    public R<java.util.List<cn.oa.service.impl.WorkflowServiceImpl.ValidationError>> validateDefinition(
            @RequestBody WfProcessDefinition definition) {
        cn.oa.service.impl.WorkflowServiceImpl.WorkflowGraph graph =
                workflowService.validateDefinition(definition.getNodeConfig());
        return R.ok(graph.errors);
    }

    @GetMapping("/definition/preview")
    @Operation(summary = "预览流程路由路径 (V1010)")
    public R<java.util.List<cn.hutool.json.JSONObject>> previewDefinition(
            @RequestParam String businessType,
            @RequestParam Long businessId,
            HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        return R.ok(workflowService.previewPath(businessType, businessId, empId));
    }
}
