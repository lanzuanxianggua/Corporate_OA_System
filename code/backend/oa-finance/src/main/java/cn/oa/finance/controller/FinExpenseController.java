package cn.oa.finance.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequirePermission;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.finance.dto.FinExpenseCreateDTO;
import cn.oa.finance.dto.FinExpenseQueryDTO;
import cn.oa.finance.service.FinExpenseService;
import cn.oa.finance.vo.FinExpenseVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 费控-费用报销 Controller
 *
 * @author oa-finance
 */
@RestController
@RequestMapping("/api/finance/expenses")
@Tag(name = "费控-费用报销")
@Slf4j
@RequiredArgsConstructor
public class FinExpenseController {

    private final FinExpenseService finExpenseService;

    /**
     * 创建报销单
     */
    @PostMapping
    @Operation(summary = "创建报销单")
    @OperationLog(module = "费控-费用报销", operation = "创建报销单")
    @RequirePermission("finance:expense:create")
    public R<Long> createExpense(
            @RequestBody @Valid FinExpenseCreateDTO dto,
            HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        if (empId == null) {
            return R.fail("用户未登录");
        }

        Long id = finExpenseService.createExpense(dto, empId);
        log.info("Expense created: id={}, empId={}", id, empId);
        return R.ok(id);
    }

    /**
     * 提交审批
     */
    @PostMapping("/{id}/actions/submit")
    @Operation(summary = "提交审批")
    @OperationLog(module = "费控-费用报销", operation = "提交审批")
    @RequirePermission("finance:expense:submit")
    public R<Void> submitToWorkflow(
            @Parameter(description = "报销单ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        if (empId == null) {
            return R.fail("用户未登录");
        }

        finExpenseService.submitToWorkflow(id, empId);
        log.info("Expense submitted: id={}, empId={}", id, empId);
        return R.ok();
    }

    /**
     * 撤回报销单
     */
    @PostMapping("/{id}/actions/revoke")
    @Operation(summary = "撤回报销单")
    @OperationLog(module = "费控-费用报销", operation = "撤回报销单")
    @RequirePermission("finance:expense:revoke")
    public R<Void> revoke(
            @Parameter(description = "报销单ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        if (empId == null) {
            return R.fail("用户未登录");
        }

        boolean isAdmin = isAdmin(request);
        finExpenseService.revoke(id, empId, isAdmin);
        log.info("Expense revoked: id={}, empId={}", id, empId);
        return R.ok();
    }

    /**
     * 分页查询报销单
     */
    @GetMapping
    @Operation(summary = "分页查询报销单")
    @RequirePermission("finance:expense:list")
    public R<PageResult<FinExpenseVO>> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "费用类别") @RequestParam(required = false) String category,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            HttpServletRequest request) {

        FinExpenseQueryDTO query = new FinExpenseQueryDTO();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setStatus(status);
        query.setCategory(category);
        query.setStartDate(startDate);
        query.setEndDate(endDate);

        Long currentEmpId = WebUtil.getEmpId(request);
        boolean isAdmin = isAdmin(request);

        IPage<FinExpenseVO> page = finExpenseService.pageQuery(query, currentEmpId, isAdmin);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    /**
     * 查询报销详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询报销详情")
    @RequirePermission("finance:expense:detail")
    public R<FinExpenseVO> getDetail(
            @Parameter(description = "报销单ID") @PathVariable Long id) {
        FinExpenseVO vo = finExpenseService.getDetail(id);
        if (vo == null) {
            return R.fail("报销单不存在");
        }
        return R.ok(vo);
    }

    /**
     * 判断当前用户是否为管理员
     */
    private boolean isAdmin(HttpServletRequest request) {
        Object isAdminAttr = request.getAttribute("isAdmin");
        if (isAdminAttr instanceof Boolean) {
            return (Boolean) isAdminAttr;
        }
        if (isAdminAttr != null) {
            return Boolean.parseBoolean(isAdminAttr.toString());
        }
        return false;
    }
}
