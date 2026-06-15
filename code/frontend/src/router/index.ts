import { createRouter, createWebHashHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useUserStore } from "@/store/user";

const Layout = () => import("@/layout/index.vue");

// ── Static routes (no auth required) ──────────────────────────────────────

const staticRoutes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/login/index.vue"),
    meta: { title: "登录", icon: "Lock" }
  },
  {
    path: "/register",
    name: "Register",
    component: () => import("@/views/login/register.vue"),
    meta: { title: "注册", icon: "User" }
  },
  {
    path: "/forgot-password",
    name: "ForgotPassword",
    component: () => import("@/views/login/forgot-password.vue"),
    meta: { title: "忘记密码", icon: "Lock" }
  },
  {
    path: "/",
    component: Layout,
    redirect: "/welcome",
    children: [
      // ── 工作台 ────────────────────────────────────────────────────────
      {
        path: "welcome",
        name: "Welcome",
        component: () => import("@/views/welcome/index.vue"),
        meta: { title: "首页", icon: "HomeFilled" }
      },
      {
        path: "oa/workbench",
        name: "Workbench",
        component: () => import("@/views/oa/workbench/index.vue"),
        meta: { title: "工作台", icon: "Monitor" }
      },
      {
        path: "oa/todo",
        name: "TodoCenter",
        component: () => import("@/views/oa/todo/index.vue"),
        meta: { title: "我的待办", icon: "List" }
      },
      {
        path: "oa/schedule/index",
        name: "ScheduleIndex",
        component: () => import("@/views/oa/schedule/index/index.vue"),
        meta: { title: "我的日程", icon: "Calendar" }
      },
      {
        path: "oa/schedule/overview",
        name: "ScheduleOverview",
        component: () => import("@/views/oa/schedule/overview/index.vue"),
        meta: { title: "日程总览", icon: "Calendar", roles: ["ADMIN"] }
      },

      // ── 考勤管理 ──────────────────────────────────────────────────────
      {
        path: "oa/attendance/clock",
        name: "AttendanceClock",
        component: () => import("@/views/oa/attendance/clock/index.vue"),
        meta: { title: "考勤打卡", icon: "Clock" }
      },
      {
        path: "oa/attendance/record",
        name: "AttendanceRecord",
        component: () => import("@/views/oa/attendance/record/index.vue"),
        meta: { title: "考勤记录", icon: "Document" }
      },
      {
        path: "oa/attendance/manage",
        name: "AttendanceManage",
        component: () => import("@/views/oa/attendance/manage/index.vue"),
        meta: { title: "考勤管理", icon: "DataBoard", roles: ["ADMIN", "DEPT_MANAGER"] }
      },
      {
        path: "oa/attendance-group",
        name: "AttendanceGroup",
        component: () => import("@/views/oa/attendance-group/index.vue"),
        meta: { title: "考勤组管理", icon: "Grid", roles: ["ADMIN"] }
      },
      {
        path: "oa/leave-balance",
        name: "LeaveBalance",
        component: () => import("@/views/oa/leave-balance/index.vue"),
        meta: { title: "假期余额", icon: "Timer" }
      },

      // ── 审批中心 ──────────────────────────────────────────────────────
      {
        path: "oa/approval-center",
        name: "ApprovalCenter",
        component: () => import("@/views/oa/approval-center/index.vue"),
        meta: { title: "审批中心", icon: "Stamp", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] }
      },

      // ── 业务申请 ──────────────────────────────────────────────────────
      {
        path: "oa/leave/apply",
        name: "LeaveApply",
        component: () => import("@/views/oa/leave/apply/index.vue"),
        meta: { title: "请假申请", icon: "EditPen" }
      },
      {
        path: "oa/leave/approval",
        name: "LeaveApproval",
        component: () => import("@/views/oa/leave/approval/index.vue"),
        meta: { title: "请假审批", icon: "Checked", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] }
      },
      {
        path: "oa/business-trip/apply",
        name: "BusinessTripApply",
        component: () => import("@/views/oa/business-trip/apply/index.vue"),
        meta: { title: "出差申请", icon: "EditPen" }
      },
      {
        path: "oa/business-trip/approval",
        name: "BusinessTripApproval",
        component: () => import("@/views/oa/business-trip/approval/index.vue"),
        meta: { title: "出差审批", icon: "Checked", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] }
      },
      {
        path: "oa/outing/apply",
        name: "OutingApply",
        component: () => import("@/views/oa/outing/apply/index.vue"),
        meta: { title: "外出申请", icon: "EditPen" }
      },
      {
        path: "oa/outing/approval",
        name: "OutingApproval",
        component: () => import("@/views/oa/outing/approval/index.vue"),
        meta: { title: "外出审批", icon: "Checked", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] }
      },
      {
        path: "oa/overtime-apply",
        name: "OvertimeApply",
        component: () => import("@/views/oa/overtime/apply/index.vue"),
        meta: { title: "加班申请", icon: "EditPen" }
      },
      {
        path: "oa/overtime-approval",
        name: "OvertimeApproval",
        component: () => import("@/views/oa/overtime/approval/index.vue"),
        meta: { title: "加班审批", icon: "Checked", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] }
      },
      {
        path: "oa/expense/apply",
        name: "ExpenseApply",
        component: () => import("@/views/oa/expense/apply/index.vue"),
        meta: { title: "报销申请", icon: "EditPen" }
      },
      {
        path: "oa/expense/approval",
        name: "ExpenseApproval",
        component: () => import("@/views/oa/expense/approval/index.vue"),
        meta: { title: "报销审批", icon: "Checked", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] }
      },
      {
        path: "oa/purchase/apply",
        name: "PurchaseApply",
        component: () => import("@/views/oa/purchase/apply/index.vue"),
        meta: { title: "采购申请", icon: "EditPen" }
      },
      {
        path: "oa/purchase/approval",
        name: "PurchaseApproval",
        component: () => import("@/views/oa/purchase/approval/index.vue"),
        meta: { title: "采购审批", icon: "Checked", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] }
      },
      {
        path: "oa/loan-apply",
        name: "LoanApply",
        component: () => import("@/views/oa/loan/apply/index.vue"),
        meta: { title: "借款申请", icon: "EditPen" }
      },
      {
        path: "oa/loan-approval",
        name: "LoanApproval",
        component: () => import("@/views/oa/loan/approval/index.vue"),
        meta: { title: "借款审批", icon: "Checked", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] }
      },

      // ── 办公协作 ──────────────────────────────────────────────────────
      {
        path: "oa/notice/list",
        name: "NoticeList",
        component: () => import("@/views/oa/notice/list/index.vue"),
        meta: { title: "通知公告", icon: "Bell" }
      },
      {
        path: "oa/notice/manage",
        name: "NoticeManage",
        component: () => import("@/views/oa/notice/manage/index.vue"),
        meta: { title: "公告管理", icon: "Bell", roles: ["ADMIN"] }
      },
      {
        path: "oa/document/list",
        name: "DocumentList",
        component: () => import("@/views/oa/document/list/index.vue"),
        meta: { title: "文档中心", icon: "Folder" }
      },
      {
        path: "oa/document/manage",
        name: "DocumentManage",
        component: () => import("@/views/oa/document/manage/index.vue"),
        meta: { title: "文档管理", icon: "Folder", roles: ["ADMIN"] }
      },
      {
        path: "oa/message/list",
        name: "MessageList",
        component: () => import("@/views/oa/message/list/index.vue"),
        meta: { title: "内部消息", icon: "ChatDotRound" }
      },
      {
        path: "oa/message/send",
        name: "MessageSend",
        component: () => import("@/views/oa/message/send/index.vue"),
        meta: { title: "发送消息", icon: "ChatDotRound", roles: ["ADMIN"] }
      },
      {
        path: "oa/meeting",
        name: "Meeting",
        component: () => import("@/views/oa/meeting/index.vue"),
        meta: { title: "会议管理", icon: "Headset" }
      },
      {
        path: "oa/meeting-room",
        name: "MeetingRoom",
        component: () => import("@/views/oa/meeting/room/index.vue"),
        meta: { title: "会议室管理", icon: "OfficeBuilding", roles: ["ADMIN"] }
      },

      // ── 业务管理（管理员扩展） ────────────────────────────────────────
      {
        path: "oa/asset",
        name: "AssetManage",
        component: () => import("@/views/oa/asset/index.vue"),
        meta: { title: "资产管理", icon: "Box", roles: ["ADMIN"] }
      },
      {
        path: "oa/asset-borrow",
        name: "AssetBorrow",
        component: () => import("@/views/oa/asset/borrow/index.vue"),
        meta: { title: "资产借用", icon: "Box" }
      },
      {
        path: "oa/contract",
        name: "ContractManage",
        component: () => import("@/views/oa/contract/index.vue"),
        meta: { title: "合同管理", icon: "Notebook", roles: ["ADMIN"] }
      },
      {
        path: "oa/budget",
        name: "BudgetManage",
        component: () => import("@/views/oa/budget/index.vue"),
        meta: { title: "预算管理", icon: "Wallet", roles: ["ADMIN"] }
      },

      // ── 人事管理 ──────────────────────────────────────────────────────
      {
        path: "oa/archive",
        name: "EmpArchive",
        component: () => import("@/views/oa/archive/index.vue"),
        meta: { title: "员工档案", icon: "UserFilled", roles: ["ADMIN"] }
      },
      {
        path: "oa/salary",
        name: "SalaryManage",
        component: () => import("@/views/oa/salary/index.vue"),
        meta: { title: "薪资管理", icon: "Money", roles: ["ADMIN"] }
      },
      {
        path: "oa/salary-my",
        name: "MySalary",
        component: () => import("@/views/oa/salary/my/index.vue"),
        meta: { title: "我的薪资", icon: "Money" }
      },

      // ── 工作流管理 ────────────────────────────────────────────────────
      {
        path: "oa/workflow-definition",
        name: "WorkflowDefinition",
        component: () => import("@/views/oa/workflow/definition/index.vue"),
        meta: { title: "流程定义", icon: "SetUp", roles: ["ADMIN"] }
      },
      {
        path: "oa/workflow-todo",
        name: "WorkflowTodo",
        component: () => import("@/views/oa/workflow/todo/index.vue"),
        meta: { title: "待办任务", icon: "Tickets", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] }
      },
      {
        path: "oa/workflow/handled",
        name: "WorkflowHandled",
        component: () => import("@/views/oa/workflow/handled/index.vue"),
        meta: { title: "已办任务", icon: "CircleCheck", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] }
      },
      {
        path: "oa/workflow/mine",
        name: "WorkflowMine",
        component: () => import("@/views/oa/workflow/mine/index.vue"),
        meta: { title: "我的申请", icon: "Document" }
      },
      {
        path: "oa/workflow/cc",
        name: "WorkflowCc",
        component: () => import("@/views/oa/workflow/cc/index.vue"),
        meta: { title: "抄送我的", icon: "CopyDocument", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] }
      },
      {
        path: "oa/workflow/delegation",
        name: "WorkflowDelegation",
        component: () => import("@/views/oa/workflow/delegation/index.vue"),
        meta: { title: "审批委派", icon: "Share", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] }
      },

      // ── 报表中心 ──────────────────────────────────────────────────────
      {
        path: "oa/report/personal",
        name: "ReportPersonal",
        component: () => import("@/views/oa/report/personal/index.vue"),
        meta: { title: "个人报表", icon: "DataAnalysis" }
      },
      {
        path: "oa/report/admin",
        name: "ReportAdmin",
        component: () => import("@/views/oa/report/admin/index.vue"),
        meta: { title: "管理报表", icon: "TrendCharts", roles: ["ADMIN", "DEPT_MANAGER"] }
      },
      {
        path: "oa/dashboard",
        name: "Dashboard",
        component: () => import("@/views/oa/dashboard/index.vue"),
        meta: { title: "数据看板", icon: "DataLine", roles: ["ADMIN"] }
      },

      // ── 预警 ──────────────────────────────────────────────────────────
      {
        path: "oa/alert-rule",
        name: "AlertRule",
        component: () => import("@/views/oa/alert/rule/index.vue"),
        meta: { title: "预警规则", icon: "Warning", roles: ["ADMIN"] }
      },
      {
        path: "oa/alert-log",
        name: "AlertLog",
        component: () => import("@/views/oa/alert/log/index.vue"),
        meta: { title: "预警记录", icon: "Warning" }
      },

      // ── 系统管理 ──────────────────────────────────────────────────────
      {
        path: "system/user",
        name: "SystemUser",
        component: () => import("@/views/system/user/index.vue"),
        meta: { title: "员工管理", icon: "User", roles: ["ADMIN"] }
      },
      {
        path: "system/dept",
        name: "SystemDept",
        component: () => import("@/views/system/dept/index.vue"),
        meta: { title: "部门管理", icon: "OfficeBuilding", roles: ["ADMIN"] }
      },
      {
        path: "system/role",
        name: "SystemRole",
        component: () => import("@/views/system/role/index.vue"),
        meta: { title: "角色管理", icon: "UserFilled", roles: ["ADMIN"] }
      },
      {
        path: "system/menu",
        name: "SystemMenu",
        component: () => import("@/views/system/menu/index.vue"),
        meta: { title: "菜单管理", icon: "Menu", roles: ["ADMIN"] }
      },
      {
        path: "system/dict",
        name: "SystemDict",
        component: () => import("@/views/system/dict/index.vue"),
        meta: { title: "字典管理", icon: "Collection", roles: ["ADMIN"] }
      },
      {
        path: "system/config",
        name: "SystemConfig",
        component: () => import("@/views/system/config/index.vue"),
        meta: { title: "参数设置", icon: "Setting", roles: ["ADMIN"] }
      },
      {
        path: "system/post",
        name: "SystemPost",
        component: () => import("@/views/system/post/index.vue"),
        meta: { title: "岗位管理", icon: "Medal", roles: ["ADMIN"] }
      },

      // ── 系统监控 ──────────────────────────────────────────────────────
      {
        path: "monitor/online",
        name: "MonitorOnline",
        component: () => import("@/views/monitor/online/index.vue"),
        meta: { title: "在线用户", icon: "Connection", roles: ["ADMIN"] }
      },
      {
        path: "monitor/logs/login",
        name: "LoginLogs",
        component: () => import("@/views/monitor/logs/login.vue"),
        meta: { title: "登录日志", icon: "Document", roles: ["ADMIN"] }
      },
      {
        path: "monitor/logs/operation",
        name: "OperationLogs",
        component: () => import("@/views/monitor/logs/operation.vue"),
        meta: { title: "操作日志", icon: "Document", roles: ["ADMIN"] }
      },
      {
        path: "monitor/logs/system",
        name: "SystemLogs",
        component: () => import("@/views/monitor/logs/system.vue"),
        meta: { title: "系统日志", icon: "Document", roles: ["ADMIN"] }
      },

      // ── 个人设置 ──────────────────────────────────────────────────────
      {
        path: "account-settings",
        name: "AccountSettings",
        component: () => import("@/views/account-settings/index.vue"),
        meta: { title: "账号设置", icon: "Setting" }
      },

      // ── 错误页 ────────────────────────────────────────────────────────
      {
        path: "error/403",
        name: "Error403",
        component: () => import("@/views/error/403.vue"),
        meta: { title: "无权限", icon: "WarningFilled" }
      },
      {
        path: "error/404",
        name: "Error404",
        component: () => import("@/views/error/404.vue"),
        meta: { title: "页面不存在", icon: "WarningFilled" }
      },
      {
        path: "error/500",
        name: "Error500",
        component: () => import("@/views/error/500.vue"),
        meta: { title: "服务器错误", icon: "WarningFilled" }
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
  routes: staticRoutes,
  scrollBehavior: () => ({ top: 0 })
});

// ── Route guards ────────────────────────────────────────────────────────

router.beforeEach((to, _from, next) => {
  // Update page title
  document.title = to.meta.title
    ? `${to.meta.title} - OA办公系统`
    : "OA办公系统";

  // Public routes
  if (to.path === "/login" || to.path === "/register" || to.path === "/forgot-password") {
    next();
    return;
  }

  // Check token existence
  const token = localStorage.getItem("token");
  if (!token) {
    next("/login");
    return;
  }

  // Validate JWT expiry
  try {
    const payload = JSON.parse(
      atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/"))
    );
    if (payload.exp && Date.now() / 1000 > payload.exp) {
      localStorage.removeItem("token");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("userInfo");
      next("/login");
      return;
    }
  } catch {
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("userInfo");
    next("/login");
    return;
  }

  // Check role-based access
  const requiredRoles = to.meta?.roles as string[] | undefined;
  if (requiredRoles?.length) {
    const userStore = useUserStore();
    if (!userStore.hasAnyRole(requiredRoles)) {
      next("/error/403");
      return;
    }
  }

  next();
});

export default router;
