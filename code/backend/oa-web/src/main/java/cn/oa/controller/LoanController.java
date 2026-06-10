package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.PageParamUtil;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaLoan;
import cn.oa.entity.SysEmployee;
import cn.oa.entity.dto.ApproveDTO;
import cn.oa.entity.dto.RepaymentDTO;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.LoanService;
import cn.oa.utils.ExcelExportUtil;
import cn.oa.vo.LoanExportVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/loan")
@Tag(name = "借支管理")
public class LoanController {

    @Autowired
    private LoanService loanService;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    private static final String[] STATUS_TEXT = {"待审批", "已通过", "已驳回", "已撤回"};

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
        IPage<OaLoan> page = loanService.pageList(PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize), empId, status);
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

    @GetMapping("/export")
    @RequireAdmin
    @Operation(summary = "导出借支数据")
    public void exportLoan(
            @RequestParam(required = false) Long empId,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws IOException {
        IPage<OaLoan> page = loanService.pageList(1, 5000, empId, status);
        List<OaLoan> records = page.getRecords();
        if (records.size() > 5000) records = records.subList(0, 5000);

        Map<Long, SysEmployee> empMap = records.stream().map(OaLoan::getEmpId)
                .filter(id -> id != null).distinct().collect(Collectors.toList()).isEmpty() ? Map.of() :
                employeeMapper.selectBatchIds(records.stream().map(OaLoan::getEmpId)
                        .filter(id -> id != null).distinct().collect(Collectors.toList()))
                        .stream().collect(Collectors.toMap(SysEmployee::getId, Function.identity()));

        List<LoanExportVO> exportList = new ArrayList<>();
        for (OaLoan r : records) {
            LoanExportVO vo = new LoanExportVO();
            SysEmployee emp = empMap.get(r.getEmpId());
            vo.setEmpName(emp != null ? emp.getEmpName() : "");
            vo.setLoanAmount(r.getLoanAmount());
            vo.setLoanReason(r.getLoanReason() != null ? r.getLoanReason() : "");
            vo.setRepaymentPlan(r.getRepaymentPlan() != null ? r.getRepaymentPlan() : "");
            int st = r.getStatus() != null ? Integer.parseInt(r.getStatus()) : -1;
            vo.setStatusText(st >= 0 && st < STATUS_TEXT.length ? STATUS_TEXT[st] : "未知");
            exportList.add(vo);
        }
        ExcelExportUtil.export(response, "借支数据", LoanExportVO.class, exportList);
    }
}
