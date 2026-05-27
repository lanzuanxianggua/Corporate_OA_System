package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaBusinessTrip;
import cn.oa.service.BusinessTripService;
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
@RequestMapping("/api/business-trip")
@Tag(name = "出差管理")
public class BusinessTripController {

    @Autowired
    private BusinessTripService businessTripService;

    @PostMapping("/submit")
    @Operation(summary = "提交出差申请")
    @OperationLog(module = "出差管理", operation = "提交出差申请")
    public R<Void> submit(@RequestBody OaBusinessTrip trip, HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        trip.setEmpId(empId);
        businessTripService.submit(trip);
        return R.ok();
    }

    @PostMapping("/approve")
    @Operation(summary = "审批出差申请")
    @OperationLog(module = "出差管理", operation = "审批出差申请")
    public R<Void> approve(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long applyId = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        Object approverIdObj = request.getAttribute("empId");
        Long approverId = (approverIdObj instanceof Number) ? ((Number) approverIdObj).longValue() : Long.valueOf(approverIdObj.toString());
        businessTripService.approve(applyId, approverId, status, remark);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询出差申请")
    public R<PageResult<OaBusinessTrip>> page(@RequestParam int pageNum,
                                               @RequestParam int pageSize,
                                               @RequestParam(required = false) Long empId,
                                               @RequestParam(required = false) Integer status) {
        IPage<OaBusinessTrip> page = businessTripService.pageList(pageNum, pageSize, empId, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }
}
