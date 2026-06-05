export interface MenuItem {
  path?: string;
  title: string;
  icon?: string;
  roles?: string[];
  children?: MenuItem[];
}

/**
 * Sidebar menu configuration organized by business domain.
 * roles: When present, the menu item is only visible to users with at least one matching role.
 * Omitting roles means the item is visible to all authenticated users.
 * The APPROVER_ROLE set is reused for approval-related items.
 */

const APPROVER_ROLES = ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"];

export const menuConfig: MenuItem[] = [
  // ── 我的工作台 ────────────────────────────────────────────────────────
  {
    title: "我的工作台",
    icon: "HomeFilled",
    children: [
      { path: "/welcome", title: "首页" },
      { path: "/oa/workbench", title: "工作台" },
      { path: "/oa/todo", title: "我的待办" },
      { path: "/oa/schedule/index", title: "我的日程" }
    ]
  },

  // ── 考勤管理 ──────────────────────────────────────────────────────────
  {
    title: "考勤管理",
    icon: "Clock",
    children: [
      { path: "/oa/attendance/clock", title: "打卡" },
      { path: "/oa/attendance/record", title: "考勤记录" },
      { path: "/oa/attendance/manage", title: "考勤管理", roles: ["ADMIN", "DEPT_MANAGER"] },
      { path: "/oa/attendance-group", title: "考勤组", roles: ["ADMIN"] },
      { path: "/oa/leave-balance", title: "假期余额" }
    ]
  },

  // ── 审批中心 ──────────────────────────────────────────────────────────
  {
    title: "审批中心",
    icon: "Stamp",
    children: [
      { path: "/oa/workflow-todo", title: "待办任务", roles: APPROVER_ROLES },
      { path: "/oa/workflow/handled", title: "已办任务", roles: APPROVER_ROLES },
      { path: "/oa/workflow/mine", title: "我的申请" },
      { path: "/oa/workflow/cc", title: "抄送我的", roles: APPROVER_ROLES },
      { path: "/oa/workflow/delegation", title: "审批委派", roles: APPROVER_ROLES },
      { path: "/oa/leave/approval", title: "请假审批", roles: APPROVER_ROLES },
      { path: "/oa/business-trip/approval", title: "出差审批", roles: APPROVER_ROLES },
      { path: "/oa/outing/approval", title: "外出审批", roles: APPROVER_ROLES },
      { path: "/oa/overtime-approval", title: "加班审批", roles: APPROVER_ROLES },
      { path: "/oa/expense/approval", title: "报销审批", roles: APPROVER_ROLES },
      { path: "/oa/purchase/approval", title: "采购审批", roles: APPROVER_ROLES },
      { path: "/oa/loan-approval", title: "借款审批", roles: APPROVER_ROLES },
      { path: "/oa/workflow-definition", title: "流程定义", roles: ["ADMIN"] }
    ]
  },

  // ── 办公协作 ──────────────────────────────────────────────────────────
  {
    title: "办公协作",
    icon: "Connection",
    children: [
      { path: "/oa/notice/list", title: "通知公告" },
      { path: "/oa/document/list", title: "文档中心" },
      { path: "/oa/message/list", title: "内部消息" },
      { path: "/oa/meeting", title: "会议管理" }
    ]
  },

  // ── 业务管理 ──────────────────────────────────────────────────────────
  {
    title: "业务管理",
    icon: "EditPen",
    children: [
      { path: "/oa/leave/apply", title: "请假" },
      { path: "/oa/business-trip/apply", title: "出差" },
      { path: "/oa/outing/apply", title: "外出" },
      { path: "/oa/overtime-apply", title: "加班" },
      { path: "/oa/expense/apply", title: "报销" },
      { path: "/oa/purchase/apply", title: "采购" },
      { path: "/oa/loan-apply", title: "借款" },
      { path: "/oa/asset-borrow", title: "资产借用" },
      { path: "/oa/contract", title: "合同管理", roles: ["ADMIN"] },
      { path: "/oa/budget", title: "预算管理", roles: ["ADMIN"] },
      { path: "/oa/asset", title: "资产管理", roles: ["ADMIN"] }
    ]
  },

  // ── 人事管理 ──────────────────────────────────────────────────────────
  {
    title: "人事管理",
    icon: "UserFilled",
    roles: ["ADMIN"],
    children: [
      { path: "/oa/archive", title: "员工档案", roles: ["ADMIN"] },
      { path: "/oa/salary", title: "薪资管理", roles: ["ADMIN"] },
      { path: "/oa/salary-my", title: "我的薪资" }
    ]
  },

  // ── 报表中心 ──────────────────────────────────────────────────────────
  {
    title: "报表中心",
    icon: "TrendCharts",
    children: [
      { path: "/oa/report/personal", title: "个人报表" },
      { path: "/oa/report/admin", title: "管理报表", roles: ["ADMIN", "DEPT_MANAGER"] },
      { path: "/oa/dashboard", title: "数据看板", roles: ["ADMIN"] }
    ]
  },

  // ── 个人中心 ──────────────────────────────────────────────────────────
  {
    title: "个人中心",
    icon: "User",
    children: [
      { path: "/oa/salary-my", title: "我的薪资" },
      { path: "/oa/alert-log", title: "预警记录" },
      { path: "/account-settings", title: "账号设置" }
    ]
  },

  // ── 系统管理 ──────────────────────────────────────────────────────────
  {
    title: "系统管理",
    icon: "Setting",
    roles: ["ADMIN"],
    children: [
      { path: "/system/user", title: "用户管理" },
      { path: "/system/dept", title: "部门管理" },
      { path: "/system/role", title: "角色管理" },
      { path: "/system/menu", title: "菜单管理" },
      { path: "/system/dict", title: "字典管理" },
      { path: "/system/config", title: "参数设置" },
      { path: "/system/post", title: "岗位管理" }
    ]
  },

  // ── 系统监控 ──────────────────────────────────────────────────────────
  {
    title: "系统监控",
    icon: "Monitor",
    roles: ["ADMIN"],
    children: [
      { path: "/monitor/online", title: "在线用户" },
      { path: "/monitor/logs/login", title: "登录日志" },
      { path: "/monitor/logs/operation", title: "操作日志" }
    ]
  }
];
