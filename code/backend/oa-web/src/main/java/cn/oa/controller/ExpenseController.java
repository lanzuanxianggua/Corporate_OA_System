package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.ExcelExportUtil;
import cn.oa.entity.OaExpense;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.ExpenseService;
import cn.oa.vo.ExpenseExportVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expense")
@Tag(name = "经费管理")
public class ExpenseController {

    private static final String[] STATUS_TEXT = {"待审批", "已通过", "已拒绝", "", "已撤回"};

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @PostMapping("/submit")
    @Operation(summary = "提交经费申请")
    @OperationLog(module = "经费管理", operation = "提交经费申请")
    public R<Void> submit(@RequestBody OaExpense expense, HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        expense.setEmpId(empId);
        expenseService.submit(expense);
        return R.ok();
    }

    @PostMapping("/approve")
    @Operation(summary = "审批经费申请")
    @OperationLog(module = "经费管理", operation = "审批经费申请")
    public R<Void> approve(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long applyId = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        Object approverIdObj = request.getAttribute("empId");
        Long approverId = (approverIdObj instanceof Number) ? ((Number) approverIdObj).longValue() : Long.valueOf(approverIdObj.toString());
        expenseService.approve(applyId, approverId, status, remark);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询经费申请")
    public R<PageResult<OaExpense>> page(@RequestParam int pageNum,
                                          @RequestParam int pageSize,
                                          @RequestParam(required = false) Long empId,
                                          @RequestParam(required = false) Integer status) {
        IPage<OaExpense> page = expenseService.pageList(pageNum, pageSize, empId, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/export")
    @RequireAdmin
    @Operation(summary = "导出经费数据")
    public void exportExpense(
            @RequestParam(required = false) Long empId,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws IOException {
        IPage<OaExpense> page = expenseService.pageList(1, 10000, empId, status);
        List<OaExpense> records = page.getRecords();
        if (records.size() > 10000) {
            records = records.subList(0, 10000);
        }

        // Build empId -> employee map
        Map<Long, SysEmployee> empMap = records.stream()
                .map(OaExpense::getEmpId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList()).isEmpty() ? Map.of() :
                employeeMapper.selectBatchIds(
                        records.stream().map(OaExpense::getEmpId)
                                .filter(id -> id != null).distinct().collect(Collectors.toList())
                ).stream().collect(Collectors.toMap(SysEmployee::getId, Function.identity()));

        List<ExpenseExportVO> exportList = new ArrayList<>();
        for (OaExpense expense : records) {
            ExpenseExportVO vo = new ExpenseExportVO();
            SysEmployee emp = empMap.get(expense.getEmpId());
            vo.setEmpName(emp != null ? emp.getEmpName() : "");
            vo.setTitle(expense.getTitle() != null ? expense.getTitle() : "");
            vo.setCategory(expense.getCategory() != null ? expense.getCategory() : "");
            vo.setAmount(expense.getAmount());
            vo.setDescription(expense.getDescription() != null ? expense.getDescription() : "");
            vo.setStatusText(expense.getStatus() != null && expense.getStatus() < STATUS_TEXT.length
                    ? STATUS_TEXT[expense.getStatus()] : "未知");
            exportList.add(vo);
        }

        ExcelExportUtil.export(response, "经费数据", ExpenseExportVO.class, exportList);
    }
}
