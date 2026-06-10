package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.PageParamUtil;
import cn.oa.entity.OaLoginLog;
import cn.oa.entity.OaOperationLog;
import cn.oa.mapper.OaLoginLogMapper;
import cn.oa.service.OperationLogService;
import cn.oa.service.OnlineUserService;
import cn.oa.vo.OnlineUserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@Tag(name = "System Monitor")
@RequireAdmin
public class MonitorController {

    @Autowired
    private OnlineUserService onlineUserService;

    @Autowired
    private OaLoginLogMapper loginLogMapper;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping("/online-logs")
    @Operation(summary = "Online users")
    public R<Map<String, Object>> onlineLogs(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        int current = PageParamUtil.pageNum(pageNum);
        int size = PageParamUtil.pageSize(pageSize);
        List<OnlineUserVO> onlineUsers = onlineUserService.getOnlineUsers();
        int from = Math.min((current - 1) * size, onlineUsers.size());
        int to = Math.min(from + size, onlineUsers.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", onlineUsers.subList(from, to));
        result.put("total", onlineUsers.size());
        result.put("pageSize", size);
        result.put("currentPage", current);
        return R.ok(result);
    }

    @PostMapping("/online-logs/{empId}/force-logout")
    @Operation(summary = "Force online user logout")
    public R<Void> forceLogout(@PathVariable Long empId) {
        onlineUserService.userLogout(empId);
        log.info("Online user forced logout: empId={}", empId);
        return R.ok();
    }

    @GetMapping("/login-logs")
    @Operation(summary = "Login logs")
    public R<Map<String, Object>> loginLogs(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize) {
        int current = PageParamUtil.pageNum(pageNum);
        int size = PageParamUtil.pageSize(pageSize);
        LambdaQueryWrapper<OaLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(OaLoginLog::getLoginTime);
        Page<OaLoginLog> page = loginLogMapper.selectPage(new Page<>(current, size), wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (OaLoginLog loginLog : page.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", loginLog.getId());
            item.put("username", loginLog.getUsername());
            item.put("ip", loginLog.getIp());
            item.put("address", "internal");
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
        result.put("pageSize", size);
        result.put("currentPage", current);
        return R.ok(result);
    }

    @GetMapping("/operation-logs")
    @Operation(summary = "Operation logs")
    public R<Map<String, Object>> operationLogs(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(required = false) String module) {
        int current = PageParamUtil.pageNum(pageNum);
        int size = PageParamUtil.pageSize(pageSize);
        PageResult<OaOperationLog> pageResult = operationLogService.pageList(current, size, module, null, null);

        List<Map<String, Object>> list = new ArrayList<>();
        for (OaOperationLog opLog : pageResult.getList()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", opLog.getId());
            item.put("username", opLog.getEmpName());
            item.put("ip", opLog.getIp());
            item.put("address", "internal");
            item.put("system", "system");
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
        result.put("pageSize", size);
        result.put("currentPage", current);
        return R.ok(result);
    }

    @GetMapping("/system-logs")
    @Operation(summary = "System logs")
    public R<Map<String, Object>> systemLogs(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        int current = PageParamUtil.pageNum(pageNum);
        int size = PageParamUtil.pageSize(pageSize);
        PageResult<OaOperationLog> pageResult = operationLogService.pageList(current, size, null, null, null);

        List<Map<String, Object>> list = new ArrayList<>();
        for (OaOperationLog opLog : pageResult.getList()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", opLog.getId());
            item.put("level", opLog.getStatus() != null && opLog.getStatus() == 1 ? 0 : 1);
            item.put("module", opLog.getModule());
            item.put("url", opLog.getRequestUrl());
            item.put("method", opLog.getMethod());
            item.put("ip", opLog.getIp());
            item.put("address", "internal");
            item.put("system", "system");
            item.put("browser", "");
            item.put("takesTime", (opLog.getCostTime() == null ? 0 : opLog.getCostTime()) + "ms");
            item.put("requestTime", opLog.getCreateTime() != null ? opLog.getCreateTime().toString() : "");
            list.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", pageResult.getTotal());
        result.put("pageSize", size);
        result.put("currentPage", current);
        return R.ok(result);
    }

    @GetMapping("/system-logs-detail")
    @Operation(summary = "System log detail")
    public R<Map<String, Object>> systemLogsDetail(@RequestParam Long id) {
        OaOperationLog opLog = operationLogService.getById(id);
        if (opLog == null) {
            return R.fail("Log not found");
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", opLog.getId());
        detail.put("level", opLog.getStatus() != null && opLog.getStatus() == 1 ? 0 : 1);
        detail.put("module", opLog.getModule());
        detail.put("operation", opLog.getOperation());
        detail.put("username", opLog.getEmpName());
        detail.put("empId", opLog.getEmpId());
        detail.put("url", opLog.getRequestUrl());
        detail.put("method", opLog.getMethod());
        detail.put("ip", opLog.getIp());
        detail.put("address", "not recorded");
        detail.put("system", "not recorded");
        detail.put("browser", "not recorded");
        detail.put("status", opLog.getStatus());
        detail.put("takesTime", (opLog.getCostTime() == null ? 0 : opLog.getCostTime()) + "ms");
        detail.put("requestTime", opLog.getCreateTime() != null ? opLog.getCreateTime().toString() : "");
        detail.put("requestHeaders", "not recorded");
        detail.put("requestBody", "not recorded");
        detail.put("responseHeaders", "not recorded");
        detail.put("responseBody", "not recorded");
        detail.put("traceId", "operation-log-" + opLog.getId());
        return R.ok(detail);
    }
}
