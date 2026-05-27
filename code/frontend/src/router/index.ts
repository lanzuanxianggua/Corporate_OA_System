import { createRouter, createWebHashHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useUserStore } from "@/store/user";

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
        path: "oa/approval-center",
        name: "ApprovalCenter",
        component: () => import("@/views/oa/approval-center/index.vue"),
        meta: { title: "审批中心", roles: ["ADMIN"] }
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
        meta: { title: "考勤管理", roles: ["ADMIN"] }
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
        meta: { title: "请假审批", roles: ["ADMIN"] }
      },
      {
        path: "oa/business-trip/apply",
        name: "BusinessTripApply",
        component: () => import("@/views/oa/business-trip/apply/index.vue"),
        meta: { title: "出差申请" }
      },
      {
        path: "oa/business-trip/approval",
        name: "BusinessTripApproval",
        component: () => import("@/views/oa/business-trip/approval/index.vue"),
        meta: { title: "出差审批", roles: ["ADMIN"] }
      },
      {
        path: "oa/outing/apply",
        name: "OutingApply",
        component: () => import("@/views/oa/outing/apply/index.vue"),
        meta: { title: "外出申请" }
      },
      {
        path: "oa/outing/approval",
        name: "OutingApproval",
        component: () => import("@/views/oa/outing/approval/index.vue"),
        meta: { title: "外出审批", roles: ["ADMIN"] }
      },
      {
        path: "oa/purchase/apply",
        name: "PurchaseApply",
        component: () => import("@/views/oa/purchase/apply/index.vue"),
        meta: { title: "采购申请" }
      },
      {
        path: "oa/purchase/approval",
        name: "PurchaseApproval",
        component: () => import("@/views/oa/purchase/approval/index.vue"),
        meta: { title: "采购审批", roles: ["ADMIN"] }
      },
      {
        path: "oa/expense/apply",
        name: "ExpenseApply",
        component: () => import("@/views/oa/expense/apply/index.vue"),
        meta: { title: "经费申请" }
      },
      {
        path: "oa/expense/approval",
        name: "ExpenseApproval",
        component: () => import("@/views/oa/expense/approval/index.vue"),
        meta: { title: "经费审批", roles: ["ADMIN"] }
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
        meta: { title: "公告管理", roles: ["ADMIN"] }
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
        meta: { title: "文档管理", roles: ["ADMIN"] }
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
        meta: { title: "日程总览", roles: ["ADMIN"] }
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
        meta: { title: "发送消息", roles: ["ADMIN"] }
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
        meta: { title: "管理员报表", roles: ["ADMIN"] }
      },
      {
        path: "oa/dashboard",
        name: "Dashboard",
        component: () => import("@/views/oa/dashboard/index.vue"),
        meta: { title: "数据看板", roles: ["ADMIN"] }
      },
      {
        path: "system/user",
        name: "SystemUser",
        component: () => import("@/views/system/user/index.vue"),
        meta: { title: "员工管理", roles: ["ADMIN"] }
      },
      {
        path: "system/role",
        name: "SystemRole",
        component: () => import("@/views/system/role/index.vue"),
        meta: { title: "角色管理", roles: ["ADMIN"] }
      },
      {
        path: "system/menu",
        name: "SystemMenu",
        component: () => import("@/views/system/menu/index.vue"),
        meta: { title: "菜单权限", roles: ["ADMIN"] }
      },
      {
        path: "system/dept",
        name: "SystemDept",
        component: () => import("@/views/system/dept/index.vue"),
        meta: { title: "部门管理", roles: ["ADMIN"] }
      },
      {
        path: "system/dict",
        name: "SystemDict",
        component: () => import("@/views/system/dict/index.vue"),
        meta: { title: "字典管理", roles: ["ADMIN"] }
      },
      {
        path: "system/config",
        name: "SystemConfig",
        component: () => import("@/views/system/config/index.vue"),
        meta: { title: "参数配置", roles: ["ADMIN"] }
      },
      {
        path: "system/post",
        name: "SystemPost",
        component: () => import("@/views/system/post/index.vue"),
        meta: { title: "岗位管理", roles: ["ADMIN"] }
      },
      {
        path: "oa/workflow-definition",
        name: "WorkflowDefinition",
        component: () => import("@/views/oa/workflow/definition/index.vue"),
        meta: { title: "工作流定义", roles: ["ADMIN"] }
      },
      {
        path: "oa/workflow-todo",
        name: "WorkflowTodo",
        component: () => import("@/views/oa/workflow/todo/index.vue"),
        meta: { title: "工作流任务" }
      },
      {
        path: "oa/workflow/cc",
        name: "WorkflowCc",
        component: () => import("@/views/oa/workflow/cc/index.vue"),
        meta: { title: "抄送记录" }
      },
      {
        path: "oa/workflow/delegation",
        name: "WorkflowDelegation",
        component: () => import("@/views/oa/workflow/delegation/index.vue"),
        meta: { title: "审批委托" }
      },
      {
        path: "oa/todo",
        name: "TodoCenter",
        component: () => import("@/views/oa/todo/index.vue"),
        meta: { title: "我的待办" }
      },
      {
        path: "oa/attendance-group",
        name: "AttendanceGroup",
        component: () => import("@/views/oa/attendance-group/index.vue"),
        meta: { title: "考勤组管理", roles: ["ADMIN"] }
      },
      {
        path: "oa/leave-balance",
        name: "LeaveBalance",
        component: () => import("@/views/oa/leave-balance/index.vue"),
        meta: { title: "假期余额" }
      },
      {
        path: "oa/overtime-apply",
        name: "OvertimeApply",
        component: () => import("@/views/oa/overtime/apply/index.vue"),
        meta: { title: "加班申请" }
      },
      {
        path: "oa/overtime-approval",
        name: "OvertimeApproval",
        component: () => import("@/views/oa/overtime/approval/index.vue"),
        meta: { title: "加班审批", roles: ["ADMIN"] }
      },
      {
        path: "oa/salary",
        name: "SalaryManage",
        component: () => import("@/views/oa/salary/index.vue"),
        meta: { title: "薪资管理", roles: ["ADMIN"] }
      },
      {
        path: "oa/salary-my",
        name: "MySalary",
        component: () => import("@/views/oa/salary/my/index.vue"),
        meta: { title: "我的薪资" }
      },
      {
        path: "oa/archive",
        name: "EmpArchive",
        component: () => import("@/views/oa/archive/index.vue"),
        meta: { title: "员工档案", roles: ["ADMIN"] }
      },
      {
        path: "oa/meeting-room",
        name: "MeetingRoom",
        component: () => import("@/views/oa/meeting/room/index.vue"),
        meta: { title: "会议室管理", roles: ["ADMIN"] }
      },
      {
        path: "oa/meeting",
        name: "Meeting",
        component: () => import("@/views/oa/meeting/index.vue"),
        meta: { title: "会议管理" }
      },
      {
        path: "oa/asset",
        name: "AssetManage",
        component: () => import("@/views/oa/asset/index.vue"),
        meta: { title: "资产管理", roles: ["ADMIN"] }
      },
      {
        path: "oa/asset-borrow",
        name: "AssetBorrow",
        component: () => import("@/views/oa/asset/borrow/index.vue"),
        meta: { title: "资产借用" }
      },
      {
        path: "oa/contract",
        name: "ContractManage",
        component: () => import("@/views/oa/contract/index.vue"),
        meta: { title: "合同管理", roles: ["ADMIN"] }
      },
      {
        path: "oa/budget",
        name: "BudgetManage",
        component: () => import("@/views/oa/budget/index.vue"),
        meta: { title: "预算管理", roles: ["ADMIN"] }
      },
      {
        path: "oa/loan-apply",
        name: "LoanApply",
        component: () => import("@/views/oa/loan/apply/index.vue"),
        meta: { title: "借支申请" }
      },
      {
        path: "oa/loan-approval",
        name: "LoanApproval",
        component: () => import("@/views/oa/loan/approval/index.vue"),
        meta: { title: "借支审批", roles: ["ADMIN"] }
      },
      {
        path: "oa/alert-rule",
        name: "AlertRule",
        component: () => import("@/views/oa/alert/rule/index.vue"),
        meta: { title: "预警规则", roles: ["ADMIN"] }
      },
      {
        path: "oa/alert-log",
        name: "AlertLog",
        component: () => import("@/views/oa/alert/log/index.vue"),
        meta: { title: "预警记录" }
      },
      {
        path: "monitor/online",
        name: "MonitorOnline",
        component: () => import("@/views/monitor/online/index.vue"),
        meta: { title: "在线用户", roles: ["ADMIN"] }
      },
      {
        path: "monitor/logs/login",
        name: "LoginLogs",
        component: () => import("@/views/monitor/logs/login.vue"),
        meta: { title: "登录日志", roles: ["ADMIN"] }
      },
      {
        path: "monitor/logs/operation",
        name: "OperationLogs",
        component: () => import("@/views/monitor/logs/operation.vue"),
        meta: { title: "操作日志", roles: ["ADMIN"] }
      },
      {
        path: "monitor/logs/system",
        name: "SystemLogs",
        component: () => import("@/views/monitor/logs/system.vue"),
        meta: { title: "系统日志", roles: ["ADMIN"] }
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
    // Check token expiry
    try {
      const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
      if (payload.exp && Date.now() / 1000 > payload.exp) {
        localStorage.removeItem("token");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("userInfo");
        next("/login");
        return;
      }
    } catch {
      // malformed token
      localStorage.removeItem("token");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("userInfo");
      next("/login");
      return;
    }

    const requiredRoles = to.meta?.roles as string[] | undefined;
    if (requiredRoles?.length) {
      const userStore = useUserStore();
      const userRoles: string[] = userStore.userInfo?.roles || [];
      const hasRole = requiredRoles.some(r => userRoles.includes(r));
      if (!hasRole) {
        next("/error/403");
        return;
      }
    }
    next();
  }
});

export default router;
