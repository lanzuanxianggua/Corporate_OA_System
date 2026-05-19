package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaOperationLog;
import cn.oa.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operation-log")
@Tag(name = "操作日志")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping("/page")
    @RequireAdmin
    @Operation(summary = "分页查询操作日志")
    public R<PageResult<OaOperationLog>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return R.ok(operationLogService.pageList(pageNum, pageSize, module, startTime, endTime));
    }
}
