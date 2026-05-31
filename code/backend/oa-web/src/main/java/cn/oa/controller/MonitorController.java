package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaLoginLog;
import cn.oa.entity.OaOperationLog;
import cn.oa.vo.OnlineUserVO;
import cn.oa.mapper.OaLoginLogMapper;
import cn.oa.service.OperationLogService;
import cn.oa.service.OnlineUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@Tag(name = "系统监控")
@RequireAdmin
public class MonitorController {

    @Autowired
    private OnlineUserService onlineUserService;

    @Autowired
    private OaLoginLogMapper loginLogMapper;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping("/online-logs")
    @Operation(summary = "在线用户日志")
    public R<Map<String, Object>> onlineLogs() {
        List<OnlineUserVO> onlineUsers = onlineUserService.getOnlineUsers();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", onlineUsers);
        result.put("total", onlineUsers.size());
        result.put("pageSize", 10);
        result.put("currentPage", 1);
        return R.ok(result);
    }

    @GetMapping("/login-logs")
    @Operation(summary = "登录日志")
    public R<Map<String, Object>> loginLogs(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<OaLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(OaLoginLog::getLoginTime);
        Page<OaLoginLog> page = loginLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (OaLoginLog loginLog : page.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", loginLog.getId());
            item.put("username", loginLog.getUsername());
            item.put("ip", loginLog.getIp());
            item.put("address", "内网");
            item.put("system", loginLog.getOs());
            item.put("browser", loginLog.getBrowser());
            item.put("status", loginLog.getStatus());
            item.put("behavior", loginLog.getMessage());
            item.put("loginTime", loginLog.getLoginTime() != null ? loginLog.getLoginTime().toString() : "");
            list.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        result.put("pageSize", pageSize);
        result.put("currentPage", pageNum);
        return R.ok(result);
    }

    @GetMapping("/operation-logs")
    @Operation(summary = "操作日志")
    public R<Map<String, Object>> operationLogs(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 @RequestParam(required = false) String module) {
        PageResult<OaOperationLog> pageResult = operationLogService.pageList(pageNum, pageSize, module, null, null);

        List<Map<String, Object>> list = new ArrayList<>();
        for (OaOperationLog opLog : pageResult.getList()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", opLog.getId());
            item.put("username", opLog.getEmpName());
            item.put("ip", opLog.getIp());
            item.put("address", "内网");
            item.put("system", "系统");
            item.put("browser", "");
            item.put("status", opLog.getStatus());
            item.put("summary", opLog.getOperation());
            item.put("module", opLog.getModule());
            item.put("operatingTime", opLog.getCreateTime() != null ? opLog.getCreateTime().toString() : "");
            list.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", pageResult.getTotal());
        result.put("pageSize", pageSize);
        result.put("currentPage", pageNum);
        return R.ok(result);
    }

    @GetMapping("/system-logs")
    @Operation(summary = "系统日志")
    public R<Map<String, Object>> systemLogs(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<OaOperationLog> pageResult = operationLogService.pageList(pageNum, pageSize, null, null, null);

        List<Map<String, Object>> list = new ArrayList<>();
        for (OaOperationLog opLog : pageResult.getList()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", opLog.getId());
            item.put("level", opLog.getStatus() == 1 ? 0 : 1);
            item.put("module", opLog.getModule());
            item.put("url", opLog.getRequestUrl());
            item.put("method", opLog.getMethod());
            item.put("ip", opLog.getIp());
            item.put("address", "内网");
            item.put("system", "系统");
            item.put("browser", "");
            item.put("takesTime", opLog.getCostTime() + "ms");
            item.put("requestTime", opLog.getCreateTime() != null ? opLog.getCreateTime().toString() : "");
            list.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", pageResult.getTotal());
        result.put("pageSize", pageSize);
        result.put("currentPage", pageNum);
        return R.ok(result);
    }

    @GetMapping("/system-logs-detail")
    @Operation(summary = "系统日志详情")
    public R<Map<String, Object>> systemLogsDetail(@RequestParam Long id) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", id);
        detail.put("level", 1);
        detail.put("module", "系统模块");
        detail.put("url", "/api/system");
        detail.put("method", "GET");
        detail.put("ip", "127.0.0.1");
        detail.put("address", "本地");
        detail.put("system", "系统");
        detail.put("browser", "");
        detail.put("takesTime", "50ms");
        detail.put("requestTime", new Date().toString());
        detail.put("requestHeaders", "Content-Type: application/json");
        detail.put("requestBody", "{}");
        detail.put("responseHeaders", "Content-Type: application/json");
        detail.put("responseBody", "{\"code\":0,\"message\":\"操作成功\"}");
        detail.put("traceId", "trace-" + id);
        return R.ok(detail);
    }
}
