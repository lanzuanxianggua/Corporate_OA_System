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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/overtime")
@Tag(name = "加班管理")
public class OvertimeController {

    @Autowired
    private OvertimeService overtimeService;

    @PostMapping("/submit")
    @Operation(summary = "提交加班申请")
    public R<Void> submit(@RequestBody OaOvertime overtime, HttpServletRequest request) {
        overtime.setEmpId((Long) request.getAttribute("empId"));
        overtimeService.submit(overtime);
        return R.ok();
    }

    @PostMapping("/approve")
    @Operation(summary = "审批加班申请")
    public R<Void> approve(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long overtimeId = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        Long approverId = (Long) request.getAttribute("empId");
        overtimeService.approve(overtimeId, approverId, status, remark);
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
