package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaLoan;
import cn.oa.service.LoanService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/loan")
@Tag(name = "借支管理")
public class LoanController {

    @Autowired
    private LoanService loanService;

    @PostMapping("/submit")
    @Operation(summary = "提交借支申请")
    public R<Void> submit(@RequestBody @Valid OaLoan loan, HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        loan.setEmpId(empId);
        loanService.submit(loan);
        log.info("Loan submitted: empId={}", empId);
        return R.ok();
    }

    @PostMapping("/approve")
    @Operation(summary = "审批借支申请")
    public R<Void> approve(@RequestBody @Valid Map<String, Object> params, HttpServletRequest request) {
        Long loanId = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        Object approverIdObj = request.getAttribute("empId");
        Long approverId = (approverIdObj instanceof Number) ? ((Number) approverIdObj).longValue() : Long.valueOf(approverIdObj.toString());
        loanService.approve(loanId, approverId, status, remark);
        log.info("Loan approved: id={}, status={}, approverId={}", loanId, status, approverId);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询借支申请")
    public R<PageResult<OaLoan>> page(@RequestParam int pageNum,
                                       @RequestParam int pageSize,
                                       @RequestParam(required = false) Long empId,
                                       @RequestParam(required = false) Integer status) {
        IPage<OaLoan> page = loanService.pageList(pageNum, pageSize, empId, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/repayment")
    @RequireAdmin
    @Operation(summary = "添加还款记录")
    public R<Void> repayment(@RequestBody @Valid Map<String, Object> params) {
        Long loanId = Long.valueOf(params.get("loanId").toString());
        java.math.BigDecimal amount = new java.math.BigDecimal(params.get("amount").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        loanService.addRepayment(loanId, amount, remark);
        log.info("Loan repayment added: loanId={}, amount={}", loanId, amount);
        return R.ok();
    }
}