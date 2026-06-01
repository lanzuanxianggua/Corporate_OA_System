package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.constant.BusinessStatus;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.AuthUtil;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaOvertime;
import cn.oa.entity.SysEmployee;
import cn.oa.entity.dto.ApproveDTO;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.OvertimeService;
import cn.oa.utils.ExcelExportUtil;
import cn.oa.vo.OvertimeExportVO;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/overtime")
@Tag(name = "加班管理")
public class OvertimeController {

    @Autowired
    private OvertimeService overtimeService;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @PostMapping("/submit")
    @Operation(summary = "提交加班申请")
    public R<Void> submit(@RequestBody @Valid OaOvertime overtime, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        overtime.setEmpId(empId);
        overtimeService.submit(overtime);
        log.info("Overtime submitted: empId={}", empId);
        return R.ok();
    }

    @PostMapping("/approve")
    @Operation(summary = "审批加班申请")
    public R<Void> approve(@RequestBody @Valid ApproveDTO dto, HttpServletRequest request) {
        Long approverId = WebUtil.getEmpId(request);
        overtimeService.approve(dto.getId(), approverId, dto.getStatus(), dto.getRemark(), dto.getTaskId());
        log.info("Overtime approved: id={}, status={}, approverId={}, taskId={}", dto.getId(), dto.getStatus(), approverId, dto.getTaskId());
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询加班申请")
    public R<PageResult<OaOvertime>> page(@RequestParam int pageNum,
                                           @RequestParam int pageSize,
                                           @RequestParam(required = false) Long empId,
                                           @RequestParam(required = false) Integer status,
                                           HttpServletRequest request) {
        Long currentEmpId = WebUtil.getEmpId(request);
        if (empId == null || !empId.equals(currentEmpId)) {
            if (!AuthUtil.isAdmin(currentEmpId)) {
                empId = currentEmpId;
            }
        }
        IPage<OaOvertime> page = overtimeService.pageList(pageNum, pageSize, empId, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/export")
    @RequireAdmin
    @Operation(summary = "导出加班数据")
    public void exportOvertime(
            @RequestParam(required = false) Long empId,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws IOException {
        IPage<OaOvertime> page = overtimeService.pageList(1, 5000, empId, status);
        List<OaOvertime> records = page.getRecords();
        if (records.size() > 5000) records = records.subList(0, 5000);

        Map<Long, SysEmployee> empMap = records.stream().map(OaOvertime::getEmpId)
                .filter(id -> id != null).distinct().collect(Collectors.toList()).isEmpty() ? Map.of() :
                employeeMapper.selectBatchIds(records.stream().map(OaOvertime::getEmpId)
                        .filter(id -> id != null).distinct().collect(Collectors.toList()))
                        .stream().collect(Collectors.toMap(SysEmployee::getId, Function.identity()));

        List<OvertimeExportVO> exportList = new ArrayList<>();
        for (OaOvertime r : records) {
            OvertimeExportVO vo = new OvertimeExportVO();
            SysEmployee emp = empMap.get(r.getEmpId());
            vo.setEmpName(emp != null ? emp.getEmpName() : "");
            vo.setOvertimeDate(r.getOvertimeDate() != null ? r.getOvertimeDate().toString() : "");
            vo.setStartTime(r.getStartTime() != null ? r.getStartTime().format(FMT) : "");
            vo.setEndTime(r.getEndTime() != null ? r.getEndTime().format(FMT) : "");
            vo.setHours(r.getHours());
            vo.setReason(r.getReason() != null ? r.getReason() : "");
            int st = r.getStatus() != null ? Integer.parseInt(r.getStatus()) : -1;
            vo.setStatusText(st >= 0 ? BusinessStatus.getLabel(st, false) : "未知");
            exportList.add(vo);
        }
        ExcelExportUtil.export(response, "加班数据", OvertimeExportVO.class, exportList);
    }

}
