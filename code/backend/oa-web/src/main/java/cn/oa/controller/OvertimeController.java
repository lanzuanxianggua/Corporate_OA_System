package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaOvertime;
import cn.oa.service.OvertimeService;
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
@RequestMapping("/api/overtime")
@Tag(name = "加班管理")
public class OvertimeController {

    @Autowired
    private OvertimeService overtimeService;

    @PostMapping("/submit")
    @Operation(summary = "提交加班申请")
    public R<Void> submit(@RequestBody @Valid OaOvertime overtime, HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        overtime.setEmpId(empId);
        overtimeService.submit(overtime);
        log.info("Overtime submitted: empId={}", empId);
        return R.ok();
    }

    @PostMapping("/approve")
    @Operation(summary = "审批加班申请")
    public R<Void> approve(@RequestBody @Valid Map<String, Object> params, HttpServletRequest request) {
        Long overtimeId = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        Long taskId = params.get("taskId") != null ? Long.valueOf(params.get("taskId").toString()) : null;
        Object approverIdObj = request.getAttribute("empId");
        Long approverId = (approverIdObj instanceof Number) ? ((Number) approverIdObj).longValue() : Long.valueOf(approverIdObj.toString());
        overtimeService.approve(overtimeId, approverId, status, remark, taskId);
        log.info("Overtime approved: id={}, status={}, approverId={}, taskId={}", overtimeId, status, approverId, taskId);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询加班申请")
    public R<PageResult<OaOvertime>> page(@RequestParam int pageNum,
                                           @RequestParam int pageSize,
                                           @RequestParam(required = false) Long empId,
                                           @RequestParam(required = false) Integer status) {
        IPage<OaOvertime> page = overtimeService.pageList(pageNum, pageSize, empId, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }
}
