package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaPurchase;
import cn.oa.service.PurchaseService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/purchase")
@Tag(name = "采购管理")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @PostMapping("/submit")
    @Operation(summary = "提交采购申请")
    @OperationLog(module = "采购管理", operation = "提交采购申请")
    public R<Void> submit(@RequestBody OaPurchase purchase, HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        purchase.setEmpId(empId);
        purchaseService.submit(purchase);
        return R.ok();
    }

    @PostMapping("/approve")
    @Operation(summary = "审批采购申请")
    @OperationLog(module = "采购管理", operation = "审批采购申请")
    public R<Void> approve(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long applyId = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        Object approverIdObj = request.getAttribute("empId");
        Long approverId = (approverIdObj instanceof Number) ? ((Number) approverIdObj).longValue() : Long.valueOf(approverIdObj.toString());
        purchaseService.approve(applyId, approverId, status, remark);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询采购申请")
    public R<PageResult<OaPurchase>> page(@RequestParam int pageNum,
                                           @RequestParam int pageSize,
                                           @RequestParam(required = false) Long empId,
                                           @RequestParam(required = false) Integer status) {
        IPage<OaPurchase> page = purchaseService.pageList(pageNum, pageSize, empId, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }
}
