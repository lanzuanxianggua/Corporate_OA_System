package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.PageParamUtil;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaPurchase;
import cn.oa.entity.SysEmployee;
import cn.oa.entity.dto.ApproveDTO;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.PurchaseService;
import cn.oa.utils.ExcelExportUtil;
import cn.oa.vo.PurchaseExportVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/purchase")
@Tag(name = "采购管理")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    private static final String[] STATUS_TEXT = {"待审批", "已通过", "已驳回", "已撤回"};

    @PostMapping("/submit")
    @Operation(summary = "提交采购申请")
    @OperationLog(module = "采购管理", operation = "提交采购申请")
    public R<Void> submit(@RequestBody @Valid OaPurchase purchase, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        purchase.setEmpId(empId);
        purchaseService.submit(purchase);
        log.info("Purchase submitted: empId={}", empId);
        return R.ok();
    }

    @PostMapping("/approve")
    @Operation(summary = "审批采购申请")
    @OperationLog(module = "采购管理", operation = "审批采购申请")
    public R<Void> approve(@RequestBody @Valid ApproveDTO dto, HttpServletRequest request) {
        Long approverId = WebUtil.getEmpId(request);
        purchaseService.approve(dto.getId(), approverId, dto.getStatus(), dto.getRemark(), dto.getTaskId());
        log.info("Purchase approved: id={}, status={}, approverId={}, taskId={}", dto.getId(), dto.getStatus(), approverId, dto.getTaskId());
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询采购申请")
    public R<PageResult<OaPurchase>> page(@RequestParam int pageNum,
                                           @RequestParam int pageSize,
                                           @RequestParam(required = false) Long empId,
                                           @RequestParam(required = false) Integer status) {
        IPage<OaPurchase> page = purchaseService.pageList(PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize), empId, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/export")
    @RequireAdmin
    @Operation(summary = "导出采购数据")
    public void exportPurchase(
            @RequestParam(required = false) Long empId,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws IOException {
        IPage<OaPurchase> page = purchaseService.pageList(1, 5000, empId, status);
        List<OaPurchase> records = page.getRecords();
        if (records.size() > 5000) records = records.subList(0, 5000);

        Map<Long, SysEmployee> empMap = records.stream().map(OaPurchase::getEmpId)
                .filter(id -> id != null).distinct().collect(Collectors.toList()).isEmpty() ? Map.of() :
                employeeMapper.selectBatchIds(records.stream().map(OaPurchase::getEmpId)
                        .filter(id -> id != null).distinct().collect(Collectors.toList()))
                        .stream().collect(Collectors.toMap(SysEmployee::getId, Function.identity()));

        List<PurchaseExportVO> exportList = new ArrayList<>();
        for (OaPurchase r : records) {
            PurchaseExportVO vo = new PurchaseExportVO();
            SysEmployee emp = empMap.get(r.getEmpId());
            vo.setEmpName(emp != null ? emp.getEmpName() : "");
            vo.setItemName(r.getItemName() != null ? r.getItemName() : "");
            vo.setQuantity(r.getQuantity());
            vo.setAmount(r.getAmount());
            vo.setReason(r.getReason() != null ? r.getReason() : "");
            vo.setStatusText(r.getStatus() != null && r.getStatus() < STATUS_TEXT.length ? STATUS_TEXT[r.getStatus()] : "未知");
            exportList.add(vo);
        }
        ExcelExportUtil.export(response, "采购数据", PurchaseExportVO.class, exportList);
    }
}
