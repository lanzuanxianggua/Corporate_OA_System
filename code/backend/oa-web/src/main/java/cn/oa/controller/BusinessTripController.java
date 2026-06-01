package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.constant.BusinessStatus;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaBusinessTrip;
import cn.oa.entity.SysEmployee;
import cn.oa.entity.dto.ApproveDTO;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.BusinessTripService;
import cn.oa.utils.ExcelExportUtil;
import cn.oa.vo.BusinessTripExportVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import java.util.function.Function;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/business-trip")
@Tag(name = "出差管理")
public class BusinessTripController {

    @Autowired
    private BusinessTripService businessTripService;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @PostMapping("/submit")
    @Operation(summary = "提交出差申请")
    @OperationLog(module = "出差管理", operation = "提交出差申请")
    public R<Void> submit(@RequestBody @Valid OaBusinessTrip trip, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        trip.setEmpId(empId);
        businessTripService.submit(trip);
        log.info("Business trip submitted: empId={}", empId);
        return R.ok();
    }

    @PostMapping("/approve")
    @Operation(summary = "审批出差申请")
    @OperationLog(module = "出差管理", operation = "审批出差申请")
    public R<Void> approve(@RequestBody @Valid ApproveDTO dto, HttpServletRequest request) {
        Long approverId = WebUtil.getEmpId(request);
        businessTripService.approve(dto.getId(), approverId, dto.getStatus(), dto.getRemark(), dto.getTaskId());
        log.info("Business trip approved: id={}, status={}, approverId={}, taskId={}", dto.getId(), dto.getStatus(), approverId, dto.getTaskId());
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询出差申请")
    public R<PageResult<OaBusinessTrip>> page(@RequestParam int pageNum,
                                               @RequestParam int pageSize,
                                               @RequestParam(required = false) Long empId,
                                               @RequestParam(required = false) Integer status,
                                               HttpServletRequest request) {
        Long currentEmpId = WebUtil.getEmpId(request);
        empId = WebUtil.enforceOwnDataAccess(currentEmpId, empId);
        IPage<OaBusinessTrip> page = businessTripService.pageList(pageNum, pageSize, empId, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/export")
    @RequireAdmin
    @Operation(summary = "导出出差数据")
    public void exportBusinessTrip(
            @RequestParam(required = false) Long empId,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws IOException {
        IPage<OaBusinessTrip> page = businessTripService.pageList(1, 5000, empId, status);
        List<OaBusinessTrip> records = page.getRecords();
        if (records.size() > 5000) records = records.subList(0, 5000);

        Map<Long, SysEmployee> empMap = records.stream().map(OaBusinessTrip::getEmpId)
                .filter(id -> id != null).distinct().collect(Collectors.toList()).isEmpty() ? Map.of() :
                employeeMapper.selectBatchIds(records.stream().map(OaBusinessTrip::getEmpId)
                        .filter(id -> id != null).distinct().collect(Collectors.toList()))
                        .stream().collect(Collectors.toMap(SysEmployee::getId, Function.identity()));

        List<BusinessTripExportVO> exportList = new ArrayList<>();
        for (OaBusinessTrip r : records) {
            BusinessTripExportVO vo = new BusinessTripExportVO();
            SysEmployee emp = empMap.get(r.getEmpId());
            vo.setEmpName(emp != null ? emp.getEmpName() : "");
            vo.setDestination(r.getDestination() != null ? r.getDestination() : "");
            vo.setPurpose(r.getPurpose() != null ? r.getPurpose() : "");
            vo.setStartTime(r.getStartTime() != null ? r.getStartTime().format(FMT) : "");
            vo.setEndTime(r.getEndTime() != null ? r.getEndTime().format(FMT) : "");
            if (r.getStartTime() != null && r.getEndTime() != null) {
                vo.setDays(String.valueOf(java.time.Duration.between(r.getStartTime(), r.getEndTime()).toDays() + 1));
            } else {
                vo.setDays("-");
            }
            vo.setStatusText(r.getStatus() != null ? BusinessStatus.getLabel(r.getStatus(), false) : "未知");
            exportList.add(vo);
        }
        ExcelExportUtil.export(response, "出差数据", BusinessTripExportVO.class, exportList);
    }

    }
