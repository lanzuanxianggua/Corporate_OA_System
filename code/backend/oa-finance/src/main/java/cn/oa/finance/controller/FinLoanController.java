package cn.oa.finance.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequirePermission;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.finance.dto.FinLoanCreateDTO;
import cn.oa.finance.dto.FinLoanRepayDTO;
import cn.oa.finance.service.FinLoanService;
import cn.oa.finance.vo.FinLoanVO;
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
 * 费控-借款管理 Controller
 *
 * @author oa-finance
 */
@RestController
@RequestMapping("/api/finance/loans")
@Tag(name = "费控-借款管理")
@Slf4j
@RequiredArgsConstructor
public class FinLoanController {

    private final FinLoanService finLoanService;

    /**
     * 创建借款申请
     */
    @PostMapping
    @Operation(summary = "创建借款申请")
    @OperationLog(module = "费控-借款管理", operation = "创建借款申请")
    @RequirePermission("finance:loan:create")
    public R<Long> createLoan(
            @RequestBody @Valid FinLoanCreateDTO dto,
            HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        if (empId == null) {
            return R.fail("用户未登录");
        }

        Long id = finLoanService.createLoan(dto, empId);
        log.info("Loan created: id={}, empId={}", id, empId);
        return R.ok(id);
    }

    /**
     * 提交审批
     */
    @PostMapping("/{id}/actions/submit")
    @Operation(summary = "提交审批")
    @OperationLog(module = "费控-借款管理", operation = "提交审批")
    @RequirePermission("finance:loan:submit")
    public R<Void> submitToWorkflow(
            @Parameter(description = "借款ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        if (empId == null) {
            return R.fail("用户未登录");
        }

        finLoanService.submitToWorkflow(id, empId);
        log.info("Loan submitted: id={}, empId={}", id, empId);
        return R.ok();
    }

    /**
     * 还款
     */
    @PostMapping("/{id}/actions/repay")
    @Operation(summary = "还款")
    @OperationLog(module = "费控-借款管理", operation = "还款")
    @RequirePermission("finance:loan:repay")
    public R<Void> repay(
            @Parameter(description = "借款ID") @PathVariable Long id,
            @RequestBody @Valid FinLoanRepayDTO dto,
            HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        if (empId == null) {
            return R.fail("用户未登录");
        }

        finLoanService.repay(id, dto, empId);
        log.info("Loan repayment: loanId={}, empId={}", id, empId);
        return R.ok();
    }

    /**
     * 分页查询借款列表
     */
    @GetMapping
    @Operation(summary = "分页查询借款列表")
    @RequirePermission("finance:loan:list")
    public R<PageResult<FinLoanVO>> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            HttpServletRequest request) {

        Long currentEmpId = WebUtil.getEmpId(request);
        boolean isAdmin = isAdmin(request);

        IPage<FinLoanVO> page = finLoanService.pageQuery(pageNum, pageSize, currentEmpId, isAdmin, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    /**
     * 查询借款详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询借款详情")
    @RequirePermission("finance:loan:detail")
    public R<FinLoanVO> getDetail(
            @Parameter(description = "借款ID") @PathVariable Long id) {
        FinLoanVO vo = finLoanService.getDetail(id);
        if (vo == null) {
            return R.fail("借款记录不存在");
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
