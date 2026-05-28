package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaOvertime;
import cn.oa.entity.dto.ApproveDTO;
import cn.oa.service.OvertimeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
                                           @RequestParam(required = false) Integer status) {
        IPage<OaOvertime> page = overtimeService.pageList(pageNum, pageSize, empId, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }
}
