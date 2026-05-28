package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaApprovalRecord;
import cn.oa.entity.WfCcRecord;
import cn.oa.entity.WfDelegation;
import cn.oa.entity.WfProcessDefinition;
import cn.oa.entity.WfProcessInstance;
import cn.oa.entity.WfTask;
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
import java.util.Map;

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
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        IPage<WfTask> page = workflowService.myPendingTasks(empId, pageNum, pageSize);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/task/handle")
    @Operation(summary = "处理任务（审批/驳回）")
    public R<Void> handleTask(@RequestBody @Valid Map<String, Object> params, HttpServletRequest request) {
        if (params.get("taskId") == null) return R.fail("taskId不能为空");
        if (params.get("status") == null && params.get("action") == null) return R.fail("status不能为空");
        Long taskId = Long.valueOf(params.get("taskId").toString());
        Object statusObj = params.get("status") != null ? params.get("status") : params.get("action");
        Integer status = Integer.valueOf(statusObj.toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        Object handlerIdObj = request.getAttribute("empId");
        Long handlerId = (handlerIdObj instanceof Number) ? ((Number) handlerIdObj).longValue() : Long.valueOf(handlerIdObj.toString());
        workflowService.handleTask(taskId, handlerId, status, remark);
        log.info("Workflow task handled: taskId={}, status={}, handlerId={}", taskId, status, handlerId);
        return R.ok();
    }

    @PostMapping("/definition/activate")
    @Operation(summary = "激活/停用流程定义")
    public R<Void> activateDefinition(@RequestBody @Valid Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        WfProcessDefinition def = workflowService.getById(id);
        if (def == null) return R.fail("流程定义不存在");
        def.setStatus("0".equals(def.getStatus()) ? "1" : "0");
        workflowService.updateById(def);
        log.info("Workflow definition activated/deactivated: id={}, status={}", id, def.getStatus());
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
    public R<Void> withdraw(@RequestBody @Valid Map<String, Object> params, HttpServletRequest request) {
        if (params.get("businessType") == null) return R.fail("businessType不能为空");
        if (params.get("businessId") == null) return R.fail("businessId不能为空");
        String businessType = params.get("businessType").toString();
        Long businessId = Long.valueOf(params.get("businessId").toString());
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        workflowService.withdrawProcess(businessType, businessId, empId);
        log.info("Workflow withdrawn: businessType={}, businessId={}, empId={}", businessType, businessId, empId);
        return R.ok();
    }

    @GetMapping("/task/find")
    @Operation(summary = "查找待处理任务")
    public R<WfTask> findTask(@RequestParam String businessType, @RequestParam Long businessId, HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        return R.ok(workflowService.findPendingTask(businessType, businessId, empId));
    }

    @PostMapping("/task/transfer")
    @Operation(summary = "转办任务")
    public R<Void> transferTask(@RequestBody @Valid Map<String, Object> params, HttpServletRequest request) {
        if (params.get("taskId") == null) return R.fail("taskId不能为空");
        if (params.get("toAssigneeId") == null) return R.fail("toAssigneeId不能为空");
        Long taskId = Long.valueOf(params.get("taskId").toString());
        Long toAssigneeId = Long.valueOf(params.get("toAssigneeId").toString());
        String reason = params.get("reason") != null ? params.get("reason").toString() : null;
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        workflowService.transferTask(taskId, empId, toAssigneeId, reason);
        log.info("Workflow task transferred: taskId={}, from={}, to={}", taskId, empId, toAssigneeId);
        return R.ok();
    }

    @PostMapping("/task/return")
    @Operation(summary = "退回任务到指定节点")
    public R<Void> returnTask(@RequestBody @Valid Map<String, Object> params, HttpServletRequest request) {
        if (params.get("taskId") == null) return R.fail("taskId不能为空");
        if (params.get("returnTarget") == null) return R.fail("returnTarget不能为空");
        Long taskId = Long.valueOf(params.get("taskId").toString());
        String returnTarget = params.get("returnTarget").toString();
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        workflowService.returnTask(taskId, empId, returnTarget, remark);
        log.info("Workflow task returned: taskId={}, target={}", taskId, returnTarget);
        return R.ok();
    }

    @PostMapping("/task/urge")
    @Operation(summary = "催办")
    public R<Void> urgeTask(@RequestBody @Valid Map<String, Object> params, HttpServletRequest request) {
        String businessType = params.get("businessType").toString();
        Long businessId = Long.valueOf(params.get("businessId").toString());
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        WfProcessInstance instance = workflowService.getByBusiness(businessType, businessId);
        if (instance == null) {
            return R.fail("流程实例不存在");
        }
        workflowService.urgeTask(instance.getId(), empId);
        return R.ok();
    }

    @GetMapping("/cc/my")
    @Operation(summary = "我的抄送")
    public R<PageResult<WfCcRecord>> myCcRecords(@RequestParam int pageNum, @RequestParam int pageSize, HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        Page<WfCcRecord> page = new Page<>(pageNum, pageSize);
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
    public R<Void> setDelegation(@RequestBody @Valid WfDelegation delegation, HttpServletRequest request) {
        Object delegatorIdObj = request.getAttribute("empId");
        Long delegatorId = (delegatorIdObj instanceof Number) ? ((Number) delegatorIdObj).longValue() : Long.valueOf(delegatorIdObj.toString());
        delegation.setDelegatorId(delegatorId);
        delegationService.setDelegation(delegation);
        log.info("Delegation set: delegatorId={}, delegateToId={}", delegatorId, delegation.getDelegateToId());
        return R.ok();
    }

    @GetMapping("/delegation/my")
    @Operation(summary = "我的审批委托")
    public R<List<WfDelegation>> myDelegations(HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        return R.ok(delegationService.getMyDelegations(empId));
    }

    @PostMapping("/delegation/cancel/{id}")
    @Operation(summary = "取消审批委托")
    public R<Void> cancelDelegation(@PathVariable Long id, HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        delegationService.cancelDelegation(id, empId);
        log.info("Delegation cancelled: id={}, empId={}", id, empId);
        return R.ok();
    }
}
