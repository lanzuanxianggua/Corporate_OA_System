import { createRouter, createWebHashHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";

const Layout = () => import("@/layout/index.vue");

const staticRoutes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/login/index.vue"),
    meta: { title: "登录" }
  },
  {
    path: "/",
    component: Layout,
    redirect: "/welcome",
    children: [
      {
        path: "welcome",
        name: "Welcome",
        component: () => import("@/views/welcome/index.vue"),
        meta: { title: "首页" }
      },
      {
        path: "oa/workbench",
        name: "Workbench",
        component: () => import("@/views/oa/workbench/index.vue"),
        meta: { title: "工作台" }
      },
      {
        path: "oa/attendance/clock",
        name: "AttendanceClock",
        component: () => import("@/views/oa/attendance/clock/index.vue"),
        meta: { title: "考勤打卡" }
      },
      {
        path: "oa/attendance/record",
        name: "AttendanceRecord",
        component: () => import("@/views/oa/attendance/record/index.vue"),
        meta: { title: "考勤记录" }
      },
      {
        path: "oa/attendance/manage",
        name: "AttendanceManage",
        component: () => import("@/views/oa/attendance/manage/index.vue"),
        meta: { title: "考勤管理", roles: ["admin"] }
      },
      {
        path: "oa/leave/apply",
        name: "LeaveApply",
        component: () => import("@/views/oa/leave/apply/index.vue"),
        meta: { title: "请假申请" }
      },
      {
        path: "oa/leave/approval",
        name: "LeaveApproval",
        component: () => import("@/views/oa/leave/approval/index.vue"),
        meta: { title: "请假审批", roles: ["admin"] }
      },
      {
        path: "oa/notice/list",
        name: "NoticeList",
        component: () => import("@/views/oa/notice/list/index.vue"),
        meta: { title: "公告通知" }
      },
      {
        path: "oa/notice/manage",
        name: "NoticeManage",
        component: () => import("@/views/oa/notice/manage/index.vue"),
        meta: { title: "公告管理", roles: ["admin"] }
      },
      {
        path: "oa/document/list",
        name: "DocumentList",
        component: () => import("@/views/oa/document/list/index.vue"),
        meta: { title: "文档中心" }
      },
      {
        path: "oa/document/manage",
        name: "DocumentManage",
        component: () => import("@/views/oa/document/manage/index.vue"),
        meta: { title: "文档管理", roles: ["admin"] }
      },
      {
        path: "oa/schedule/index",
        name: "ScheduleIndex",
        component: () => import("@/views/oa/schedule/index/index.vue"),
        meta: { title: "我的日程" }
      },
      {
        path: "oa/schedule/overview",
        name: "ScheduleOverview",
        component: () => import("@/views/oa/schedule/overview/index.vue"),
        meta: { title: "日程总览", roles: ["admin"] }
      },
      {
        path: "oa/message/list",
        name: "MessageList",
        component: () => import("@/views/oa/message/list/index.vue"),
        meta: { title: "消息中心" }
      },
      {
        path: "oa/message/send",
        name: "MessageSend",
        component: () => import("@/views/oa/message/send/index.vue"),
        meta: { title: "发送消息", roles: ["admin"] }
      },
      {
        path: "oa/report/personal",
        name: "ReportPersonal",
        component: () => import("@/views/oa/report/personal/index.vue"),
        meta: { title: "个人报表" }
      },
      {
        path: "oa/report/admin",
        name: "ReportAdmin",
        component: () => import("@/views/oa/report/admin/index.vue"),
        meta: { title: "管理员报表", roles: ["admin"] }
      },
      {
        path: "oa/dashboard",
        name: "Dashboard",
        component: () => import("@/views/oa/dashboard/index.vue"),
        meta: { title: "数据看板", roles: ["admin"] }
      },
      {
        path: "system/user",
        name: "SystemUser",
        component: () => import("@/views/system/user/index.vue"),
        meta: { title: "员工管理", roles: ["admin"] }
      },
      {
        path: "system/role",
        name: "SystemRole",
        component: () => import("@/views/system/role/index.vue"),
        meta: { title: "角色管理", roles: ["admin"] }
      },
      {
        path: "system/menu",
        name: "SystemMenu",
        component: () => import("@/views/system/menu/index.vue"),
        meta: { title: "菜单管理", roles: ["admin"] }
      },
      {
        path: "system/dept",
        name: "SystemDept",
        component: () => import("@/views/system/dept/index.vue"),
        meta: { title: "部门管理", roles: ["admin"] }
      },
      {
        path: "monitor/online",
        name: "MonitorOnline",
        component: () => import("@/views/monitor/online/index.vue"),
        meta: { title: "在线用户", roles: ["admin"] }
      },
      {
        path: "monitor/logs/login",
        name: "LoginLogs",
        component: () => import("@/views/monitor/logs/login.vue"),
        meta: { title: "登录日志", roles: ["admin"] }
      },
      {
        path: "monitor/logs/operation",
        name: "OperationLogs",
        component: () => import("@/views/monitor/logs/operation.vue"),
        meta: { title: "操作日志", roles: ["admin"] }
      },
      {
        path: "monitor/logs/system",
        name: "SystemLogs",
        component: () => import("@/views/monitor/logs/system.vue"),
        meta: { title: "系统日志", roles: ["admin"] }
      },
      {
        path: "account-settings",
        name: "AccountSettings",
        component: () => import("@/views/account-settings/index.vue"),
        meta: { title: "账号设置" }
      },
      {
        path: "error/403",
        name: "Error403",
        component: () => import("@/views/error/403.vue"),
        meta: { title: "无权限" }
      },
      {
        path: "error/404",
        name: "Error404",
        component: () => import("@/views/error/404.vue"),
        meta: { title: "页面不存在" }
      },
      {
        path: "error/500",
        name: "Error500",
        component: () => import("@/views/error/500.vue"),
        meta: { title: "服务器错误" }
      }
    ]
  },
  {
    path: "/:pathMatch(.*)*",
    redirect: "/error/404"
  }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes: staticRoutes
});

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem("token");
  document.title = to.meta.title
    ? `${to.meta.title} - OA办公系统`
    : "OA办公系统";

  if (to.path === "/login") {
    next();
  } else if (!token) {
    next("/login");
  } else {
    next();
  }
});

export default router;
