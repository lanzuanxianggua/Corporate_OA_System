package cn.oa.controller;

import cn.oa.common.result.R;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin
public class RouteController {

    @GetMapping("/get-async-routes")
    public R<List<Map<String, Object>>> getAsyncRoutes() {
        List<Map<String, Object>> routes = new ArrayList<>();

        // ===== OA 办公模块 =====
        Map<String, Object> oa = new LinkedHashMap<>();
        oa.put("path", "/oa");
        Map<String, Object> oaMeta = new LinkedHashMap<>();
        oaMeta.put("icon", "ri:building-line");
        oaMeta.put("title", "menus.pureOa");
        oaMeta.put("rank", 1);
        oa.put("meta", oaMeta);
        List<Map<String, Object>> oaChildren = new ArrayList<>();

        // 工作台
        oaChildren.add(buildRoute("/oa/workbench/index", "OaWorkbench", "ri:dashboard-line", "menus.pureWorkbench", null));

        // 我的考勤
        oaChildren.add(buildRoute("/oa/attendance/clock/index", "OaAttendanceClock", "ri:time-line", "menus.pureAttendanceClock", null));

        // 考勤记录
        oaChildren.add(buildRoute("/oa/attendance/record/index", "OaAttendanceRecord", "ri:file-list-line", "menus.pureAttendanceRecord", null));

        // 请假申请
        oaChildren.add(buildRoute("/oa/leave/apply/index", "OaLeaveApply", "ri:calendar-check-line", "menus.pureLeaveApply", null));

        // 公告通知
        oaChildren.add(buildRoute("/oa/notice/list/index", "OaNoticeList", "ri:notification-line", "menus.pureNotice", null));

        // 文档中心
        oaChildren.add(buildRoute("/oa/document/list/index", "OaDocumentList", "ri:folder-line", "menus.pureDocument", null));

        // 我的日程
        oaChildren.add(buildRoute("/oa/schedule/index/index", "OaScheduleIndex", "ri:calendar-line", "menus.pureSchedule", null));

        // 消息中心
        oaChildren.add(buildRoute("/oa/message/list/index", "OaMessageList", "ri:chat-3-line", "menus.pureMessage", null));

        // 个人报表
        oaChildren.add(buildRoute("/oa/report/personal/index", "OaReportPersonal", "ri:bar-chart-line", "menus.purePersonalReport", null));

        oa.put("children", oaChildren);
        routes.add(oa);

        // ===== OA 管理（仅管理员） =====
        Map<String, Object> oaAdmin = new LinkedHashMap<>();
        oaAdmin.put("path", "/oa-admin");
        Map<String, Object> oaAdminMeta = new LinkedHashMap<>();
        oaAdminMeta.put("icon", "ri:shield-check-line");
        oaAdminMeta.put("title", "menus.pureOaAdmin");
        oaAdminMeta.put("rank", 2);
        oaAdminMeta.put("roles", List.of("ADMIN"));
        oaAdmin.put("meta", oaAdminMeta);
        List<Map<String, Object>> oaAdminChildren = new ArrayList<>();

        // 数据看板
        oaAdminChildren.add(buildRoute("/oa/dashboard/index", "OaDashboard", "ri:dashboard-2-line", "menus.pureDashboard", List.of("ADMIN")));

        // 考勤管理
        oaAdminChildren.add(buildRoute("/oa/attendance/manage/index", "OaAttendanceManage", "ri:time-line", "menus.pureAttendanceManage", List.of("ADMIN")));

        // 请假审批
        oaAdminChildren.add(buildRoute("/oa/leave/approval/index", "OaLeaveApproval", "ri:checkbox-circle-line", "menus.pureLeaveApproval", List.of("ADMIN")));

        // 公告管理
        oaAdminChildren.add(buildRoute("/oa/notice/manage/index", "OaNoticeManage", "ri:notification-badge-line", "menus.pureNoticeManage", List.of("ADMIN")));

        // 文档管理
        oaAdminChildren.add(buildRoute("/oa/document/manage/index", "OaDocumentManage", "ri:folder-shield-line", "menus.pureDocumentManage", List.of("ADMIN")));

        // 日程查看
        oaAdminChildren.add(buildRoute("/oa/schedule/overview/index", "OaScheduleOverview", "ri:calendar-2-line", "menus.pureScheduleOverview", List.of("ADMIN")));

        // 消息发送
        oaAdminChildren.add(buildRoute("/oa/message/send/index", "OaMessageSend", "ri:send-plane-line", "menus.pureMessageSend", List.of("ADMIN")));

        // 数据报表
        oaAdminChildren.add(buildRoute("/oa/report/admin/index", "OaReportAdmin", "ri:pie-chart-line", "menus.pureAdminReport", List.of("ADMIN")));

        oaAdmin.put("children", oaAdminChildren);
        routes.add(oaAdmin);

        // ===== 系统管理（仅管理员） =====
        Map<String, Object> system = new LinkedHashMap<>();
        system.put("path", "/system");
        Map<String, Object> systemMeta = new LinkedHashMap<>();
        systemMeta.put("icon", "ri:settings-3-line");
        systemMeta.put("title", "menus.pureSysManagement");
        systemMeta.put("rank", 10);
        systemMeta.put("roles", List.of("ADMIN"));
        system.put("meta", systemMeta);
        List<Map<String, Object>> systemChildren = new ArrayList<>();

        systemChildren.add(buildRoute("/system/user/index", "SystemUser", "ri:admin-line", "menus.pureUser", List.of("ADMIN")));
        systemChildren.add(buildRoute("/system/role/index", "SystemRole", "ri:admin-line", "menus.pureRole", List.of("ADMIN")));
        systemChildren.add(buildRoute("/system/menu/index", "SystemMenu", "ri:admin-line", "menus.pureMenu", List.of("ADMIN")));
        systemChildren.add(buildRoute("/system/dept/index", "SystemDept", "ri:admin-line", "menus.pureDept", List.of("ADMIN")));

        system.put("children", systemChildren);
        routes.add(system);

        // ===== 系统监控（仅管理员） =====
        Map<String, Object> monitor = new LinkedHashMap<>();
        monitor.put("path", "/monitor");
        Map<String, Object> monitorMeta = new LinkedHashMap<>();
        monitorMeta.put("icon", "ri:eye-line");
        monitorMeta.put("title", "menus.pureSysMonitor");
        monitorMeta.put("rank", 11);
        monitorMeta.put("roles", List.of("ADMIN"));
        monitor.put("meta", monitorMeta);
        List<Map<String, Object>> monitorChildren = new ArrayList<>();

        monitorChildren.add(buildRoute("/monitor/online/index", "MonitorOnline", "ri:eye-line", "menus.pureOnlineUser", List.of("ADMIN")));

        Map<String, Object> logs = new LinkedHashMap<>();
        logs.put("path", "/monitor/logs");
        logs.put("name", "MonitorLogs");
        Map<String, Object> logsMeta = new LinkedHashMap<>();
        logsMeta.put("icon", "ri:file-list-line");
        logsMeta.put("title", "menus.pureLog");
        logsMeta.put("roles", List.of("ADMIN"));
        logs.put("meta", logsMeta);
        List<Map<String, Object>> logsChildren = new ArrayList<>();

        Map<String, Object> loginLog = buildRoute("/monitor/logs/login", "MonitorLogsLogin", null, "menus.pureLoginLog", null);
        logsChildren.add(loginLog);

        Map<String, Object> opLog = buildRoute("/monitor/logs/operation", "MonitorLogsOperation", null, "menus.pureOperationLog", null);
        logsChildren.add(opLog);

        Map<String, Object> sysLog = buildRoute("/monitor/logs/system", "MonitorLogsSystem", null, "menus.pureSystemLog", null);
        logsChildren.add(sysLog);

        logs.put("children", logsChildren);
        monitorChildren.add(logs);
        monitor.put("children", monitorChildren);
        routes.add(monitor);

        return R.ok(routes);
    }

    private Map<String, Object> buildRoute(String path, String name, String icon, String title, List<String> roles) {
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("path", path);
        route.put("name", name);
        Map<String, Object> meta = new LinkedHashMap<>();
        if (icon != null) meta.put("icon", icon);
        meta.put("title", title);
        if (roles != null) meta.put("roles", roles);
        route.put("meta", meta);
        return route;
    }
}
