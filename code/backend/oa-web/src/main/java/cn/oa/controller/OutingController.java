package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaOuting;
import cn.oa.entity.dto.ApproveDTO;
import cn.oa.service.OutingService;
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

@Slf4j
@RestController
@RequestMapping("/api/outing")
@Tag(name = "外出管理")
public class OutingController {

    @Autowired
    private OutingService outingService;

    @PostMapping("/submit")
    @Operation(summary = "提交外出申请")
    @OperationLog(module = "外出管理", operation = "提交外出申请")
    public R<Void> submit(@RequestBody @Valid OaOuting outing, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        outing.setEmpId(empId);
        outingService.submit(outing);
        log.info("Outing submitted: empId={}", empId);
        return R.ok();
    }

    @PostMapping("/approve")
    @Operation(summary = "审批外出申请")
    @OperationLog(module = "外出管理", operation = "审批外出申请")
    public R<Void> approve(@RequestBody @Valid ApproveDTO dto, HttpServletRequest request) {
        Long approverId = WebUtil.getEmpId(request);
        outingService.approve(dto.getId(), approverId, dto.getStatus(), dto.getRemark(), dto.getTaskId());
        log.info("Outing approved: id={}, status={}, approverId={}, taskId={}", dto.getId(), dto.getStatus(), approverId, dto.getTaskId());
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
