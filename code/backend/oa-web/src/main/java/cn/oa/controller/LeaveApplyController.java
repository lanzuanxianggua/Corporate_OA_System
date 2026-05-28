package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.ExcelExportUtil;
import cn.oa.entity.OaLeaveApply;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.LeaveApplyService;
import cn.oa.vo.LeaveExportVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leave")
@Tag(name = "请假管理")
@Slf4j
@SuppressWarnings("deprecation")
public class LeaveApplyController {

    private static final String[] LEAVE_TYPE_TEXT = {"", "年假", "事假", "病假", "婚假", "产假", "丧假"};
    private static final String[] STATUS_TEXT = {"待审批", "已通过", "已拒绝", "", "已撤回"};
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private LeaveApplyService leaveApplyService;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @PostMapping("/submit")
    @Operation(summary = "提交请假申请")
    @OperationLog(module = "请假管理", operation = "提交请假申请")
    public R<Void> submit(@RequestBody @Valid OaLeaveApply apply, HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        apply.setEmpId(empId);
        leaveApplyService.submit(apply);
        log.info("Leave submitted: empId={}", empId);
        return R.ok();
    }

    @PostMapping("/approve")
    @Operation(summary = "审批请假申请")
    @OperationLog(module = "请假管理", operation = "审批请假申请")
    public R<Void> approve(@RequestBody @Valid Map<String, Object> params, HttpServletRequest request) {
        Long applyId = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        Long taskId = params.get("taskId") != null ? Long.valueOf(params.get("taskId").toString()) : null;
        Object approverIdObj = request.getAttribute("empId");
        Long approverId = (approverIdObj instanceof Number) ? ((Number) approverIdObj).longValue() : Long.valueOf(approverIdObj.toString());
        leaveApplyService.approve(applyId, approverId, status, remark, taskId);
        log.info("Leave approved: id={}, status={}, approverId={}, taskId={}", applyId, status, approverId, taskId);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询请假申请")
    public R<PageResult<OaLeaveApply>> page(@RequestParam int pageNum,
                                            @RequestParam int pageSize,
                                            @RequestParam(required = false) Long empId,
                                            @RequestParam(required = false) Integer status) {
        IPage<OaLeaveApply> page = leaveApplyService.pageList(pageNum, pageSize, empId, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/export")
    @RequireAdmin
    @Operation(summary = "导出请假数据")
    public void exportLeave(
            @RequestParam(required = false) Long empId,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws IOException {
        // Export with reasonable limit to prevent OOM
        IPage<OaLeaveApply> page = leaveApplyService.pageList(1, 5000, empId, status);
        List<OaLeaveApply> records = page.getRecords();
        if (records.size() > 1000) {
            log.warn("Export result count: {}, consider async export", records.size());
        }
        if (records.size() > 5000) {
            records = records.subList(0, 5000);
        }

        // Build empId -> employee map
        Map<Long, SysEmployee> empMap = records.stream()
                .map(OaLeaveApply::getEmpId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList()).isEmpty() ? Map.of() :
                employeeMapper.selectBatchIds(
                        records.stream().map(OaLeaveApply::getEmpId)
                                .filter(id -> id != null).distinct().collect(Collectors.toList())
                ).stream().collect(Collectors.toMap(SysEmployee::getId, Function.identity()));

        List<LeaveExportVO> exportList = new ArrayList<>();
        for (OaLeaveApply leave : records) {
            LeaveExportVO vo = new LeaveExportVO();
            SysEmployee emp = empMap.get(leave.getEmpId());
            vo.setEmpName(emp != null ? emp.getEmpName() : "");
            vo.setLeaveType(leave.getLeaveType() != null && leave.getLeaveType() < LEAVE_TYPE_TEXT.length
                    ? LEAVE_TYPE_TEXT[leave.getLeaveType()] : "其他");
            vo.setStartTime(leave.getStartTime() != null ? leave.getStartTime().format(DATETIME_FMT) : "");
            vo.setEndTime(leave.getEndTime() != null ? leave.getEndTime().format(DATETIME_FMT) : "");
            vo.setDays(calculateDays(leave));
            vo.setReason(leave.getReason() != null ? leave.getReason() : "");
            vo.setStatusText(leave.getStatus() != null && leave.getStatus() < STATUS_TEXT.length
                    ? STATUS_TEXT[leave.getStatus()] : "未知");
            exportList.add(vo);
        }

        ExcelExportUtil.export(response, "请假数据", LeaveExportVO.class, exportList);
    }

    private BigDecimal calculateDays(OaLeaveApply leave) {
        if (leave.getStartTime() == null || leave.getEndTime() == null) return BigDecimal.ZERO;
        long days = java.time.Duration.between(leave.getStartTime(), leave.getEndTime()).toDays() + 1;
        return BigDecimal.valueOf(Math.max(days, 0));
    }
}
