package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaOuting;
import cn.oa.service.OutingService;
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
@RequestMapping("/api/outing")
@Tag(name = "外出管理")
public class OutingController {

    @Autowired
    private OutingService outingService;

    @PostMapping("/submit")
    @Operation(summary = "提交外出申请")
    @OperationLog(module = "外出管理", operation = "提交外出申请")
    public R<Void> submit(@RequestBody OaOuting outing, HttpServletRequest request) {
        Object empIdObj = request.getAttribute("empId");
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : Long.valueOf(empIdObj.toString());
        outing.setEmpId(empId);
        outingService.submit(outing);
        return R.ok();
    }

    @PostMapping("/approve")
    @Operation(summary = "审批外出申请")
    @OperationLog(module = "外出管理", operation = "审批外出申请")
    public R<Void> approve(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long applyId = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        Object approverIdObj = request.getAttribute("empId");
        Long approverId = (approverIdObj instanceof Number) ? ((Number) approverIdObj).longValue() : Long.valueOf(approverIdObj.toString());
        outingService.approve(applyId, approverId, status, remark);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询外出申请")
    public R<PageResult<OaOuting>> page(@RequestParam int pageNum,
                                         @RequestParam int pageSize,
                                         @RequestParam(required = false) Long empId,
                                         @RequestParam(required = false) Integer status) {
        IPage<OaOuting> page = outingService.pageList(pageNum, pageSize, empId, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }
}
