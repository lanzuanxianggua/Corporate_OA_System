package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaLoan;
import cn.oa.entity.dto.ApproveDTO;
import cn.oa.entity.dto.RepaymentDTO;
import cn.oa.service.LoanService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
        Long empId = WebUtil.getEmpId(request);
        loan.setEmpId(empId);
        loanService.submit(loan);
        log.info("Loan submitted: empId={}", empId);
        return R.ok();
    }

    @PostMapping("/approve")
    @Operation(summary = "审批借支申请")
    public R<Void> approve(@RequestBody @Valid ApproveDTO dto, HttpServletRequest request) {
        Long approverId = WebUtil.getEmpId(request);
        loanService.approve(dto.getId(), approverId, dto.getStatus(), dto.getRemark(), dto.getTaskId());
        log.info("Loan approved: id={}, status={}, approverId={}, taskId={}", dto.getId(), dto.getStatus(), approverId, dto.getTaskId());
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
    public R<Void> repayment(@RequestBody @Valid RepaymentDTO dto) {
        loanService.addRepayment(dto.getLoanId(), dto.getAmount(), dto.getRemark());
        log.info("Loan repayment added: loanId={}, amount={}", dto.getLoanId(), dto.getAmount());
        return R.ok();
    }
}
