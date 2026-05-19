package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaLeaveApply;
import cn.oa.service.LeaveApplyService;
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
@RequestMapping("/api/leave")
@Tag(name = "请假管理")
public class LeaveApplyController {

    @Autowired
    private LeaveApplyService leaveApplyService;

    @PostMapping("/submit")
    @Operation(summary = "提交请假申请")
    public R<Void> submit(@RequestBody OaLeaveApply apply) {
        leaveApplyService.submit(apply);
        return R.ok();
    }

    @PostMapping("/approve")
    @RequireAdmin
    @Operation(summary = "审批请假申请")
    public R<Void> approve(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long applyId = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        Long approverId = (Long) request.getAttribute("empId");
        leaveApplyService.approve(applyId, approverId, status, remark);
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
}
