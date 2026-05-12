package cn.oa.controller;

import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaLoginLog;
import cn.oa.entity.OaOperationLog;
import cn.oa.entity.OnlineUserVO;
import cn.oa.mapper.OaLoginLogMapper;
import cn.oa.service.OperationLogService;
import cn.oa.service.OnlineUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin
@Tag(name = "系统监控")
public class MonitorController {

    @Autowired
    private OnlineUserService onlineUserService;

    @Autowired
    private OaLoginLogMapper loginLogMapper;

    @Autowired
    private OperationLogService operationLogService;

    @PostMapping("/online-logs")
    public R<Map<String, Object>> onlineLogs(@RequestBody(required = false) Map<String, Object> params) {
        List<OnlineUserVO> onlineUsers = onlineUserService.getOnlineUsers();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", onlineUsers);
        result.put("total", onlineUsers.size());
        result.put("pageSize", 10);
        result.put("currentPage", 1);
        return R.ok(result);
    }

    @PostMapping("/login-logs")
    public R<Map<String, Object>> loginLogs(@RequestBody(required = false) Map<String, Object> params) {
        int pageNum = params != null && params.get("page") != null ? ((Number) params.get("page")).intValue() : 1;
        int pageSize = params != null && params.get("pageSize") != null ? ((Number) params.get("pageSize")).intValue() : 10;

        LambdaQueryWrapper<OaLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(OaLoginLog::getLoginTime);
        Page<OaLoginLog> page = loginLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (OaLoginLog log : page.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", log.getId());
            item.put("username", log.getUsername());
            item.put("ip", log.getIp());
            item.put("address", "内网");
            item.put("system", log.getOs());
            item.put("browser", log.getBrowser());
            item.put("status", log.getStatus());
            item.put("behavior", log.getMessage());
            item.put("loginTime", log.getLoginTime() != null ? log.getLoginTime().toString() : "");
            list.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        result.put("pageSize", pageSize);
        result.put("currentPage", pageNum);
        return R.ok(result);
    }

    @PostMapping("/operation-logs")
    public R<Map<String, Object>> operationLogs(@RequestBody(required = false) Map<String, Object> params) {
        int pageNum = params != null && params.get("page") != null ? ((Number) params.get("page")).intValue() : 1;
        int pageSize = params != null && params.get("pageSize") != null ? ((Number) params.get("pageSize")).intValue() : 10;

        String module = params != null && params.get("module") != null ? params.get("module").toString() : null;
        PageResult<OaOperationLog> pageResult = operationLogService.pageList(pageNum, pageSize, module, null, null);

        List<Map<String, Object>> list = new ArrayList<>();
        for (OaOperationLog log : pageResult.getList()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", log.getId());
            item.put("username", log.getEmpName());
            item.put("ip", log.getIp());
            item.put("address", "内网");
            item.put("system", "系统");
            item.put("browser", "");
            item.put("status", log.getStatus());
            item.put("summary", log.getOperation());
            item.put("module", log.getModule());
            item.put("operatingTime", log.getCreateTime() != null ? log.getCreateTime().toString() : "");
            list.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", pageResult.getTotal());
        result.put("pageSize", pageSize);
        result.put("currentPage", pageNum);
        return R.ok(result);
    }

    @PostMapping("/system-logs")
    public R<Map<String, Object>> systemLogs(@RequestBody(required = false) Map<String, Object> params) {
        // 系统日志暂时使用操作日志的数据
        int pageNum = params != null && params.get("page") != null ? ((Number) params.get("page")).intValue() : 1;
        int pageSize = params != null && params.get("pageSize") != null ? ((Number) params.get("pageSize")).intValue() : 10;

        PageResult<OaOperationLog> pageResult = operationLogService.pageList(pageNum, pageSize, null, null, null);

        List<Map<String, Object>> list = new ArrayList<>();
        for (OaOperationLog log : pageResult.getList()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", log.getId());
            item.put("level", log.getStatus() == 1 ? 0 : 1);
            item.put("module", log.getModule());
            item.put("url", log.getRequestUrl());
            item.put("method", log.getMethod());
            item.put("ip", log.getIp());
            item.put("address", "内网");
            item.put("system", "系统");
            item.put("browser", "");
            item.put("takesTime", log.getCostTime() + "ms");
            item.put("requestTime", log.getCreateTime() != null ? log.getCreateTime().toString() : "");
            list.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", pageResult.getTotal());
        result.put("pageSize", pageSize);
        result.put("currentPage", pageNum);
        return R.ok(result);
    }

    @PostMapping("/system-logs-detail")
    public R<Map<String, Object>> systemLogsDetail(@RequestBody Map<String, Object> params) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", params.get("id"));
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
        detail.put("traceId", "trace-" + params.get("id"));
        return R.ok(detail);
    }
}
