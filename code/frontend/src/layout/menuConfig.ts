export interface MenuItem {
  path?: string;
  title: string;
  icon?: string;
  roles?: string[];
  children?: MenuItem[];
}

export const menuConfig: MenuItem[] = [
  {
    path: "/welcome",
    title: "首页",
    icon: "HomeFilled"
  },
  {
    title: "我的申请",
    icon: "EditPen",
    children: [
      { path: "/oa/leave/apply", title: "请假申请" },
      { path: "/oa/business-trip/apply", title: "出差申请" },
      { path: "/oa/outing/apply", title: "外出申请" },
      { path: "/oa/overtime-apply", title: "加班申请" },
      { path: "/oa/expense/apply", title: "经费申请" },
      { path: "/oa/purchase/apply", title: "采购申请" },
      { path: "/oa/loan-apply", title: "借支申请" }
    ]
  },
  {
    path: "/oa/approval-center",
    title: "审批中心",
    icon: "Stamp",
    roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"]
  },
  {
    title: "考勤管理",
    icon: "Clock",
    children: [
      { path: "/oa/attendance/clock", title: "考勤打卡" },
      { path: "/oa/attendance/record", title: "考勤记录" },
      { path: "/oa/attendance/manage", title: "考勤管理", roles: ["ADMIN", "DEPT_MANAGER"] },
      { path: "/oa/attendance-group", title: "考勤组管理", roles: ["ADMIN"] },
      { path: "/oa/leave-balance", title: "假期余额" }
    ]
  },
  {
    title: "办公协作",
    icon: "Connection",
    children: [
      { path: "/oa/notice/list", title: "公告通知" },
      { path: "/oa/document/list", title: "文档中心" },
      { path: "/oa/message/list", title: "消息中心" },
      { path: "/oa/schedule/index", title: "我的日程" },
      { path: "/oa/meeting", title: "会议管理" },
      { path: "/oa/asset-borrow", title: "资产借用" }
    ]
  },
  {
    title: "个人中心",
    icon: "User",
    children: [
      { path: "/oa/salary-my", title: "我的薪资" },
      { path: "/oa/todo", title: "我的待办" },
      { path: "/oa/alert-log", title: "预警记录" },
      { path: "/oa/report/personal", title: "个人报表" }
    ]
  },
  {
    title: "管理后台",
    icon: "DataAnalysis",
    roles: ["ADMIN"],
    children: [
      { path: "/oa/dashboard", title: "数据看板" },
      { path: "/oa/attendance/manage", title: "考勤管理" },
      { path: "/oa/attendance-group", title: "考勤组管理" },
      { path: "/oa/archive", title: "员工档案" },
      { path: "/oa/salary", title: "薪资管理" },
      { path: "/oa/contract", title: "合同管理" },
      { path: "/oa/asset", title: "资产管理" },
      { path: "/oa/budget", title: "预算管理" },
      { path: "/oa/meeting-room", title: "会议室管理" },
      { path: "/oa/notice/manage", title: "公告管理" },
      { path: "/oa/document/manage", title: "文档管理" },
      { path: "/oa/schedule/overview", title: "日程总览" },
      { path: "/oa/message/send", title: "发送消息" },
      { path: "/oa/alert-rule", title: "预警规则" }
    ]
  },
  {
    title: "工作流管理",
    icon: "SetUp",
    children: [
      { path: "/oa/workflow-definition", title: "流程定义", roles: ["ADMIN"] },
      { path: "/oa/workflow/cc", title: "抄送记录", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] },
      { path: "/oa/workflow/delegation", title: "审批委托", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] },
      { path: "/oa/workflow-todo", title: "待办任务", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] }
    ]
  },
  {
    title: "审批管理",
    icon: "Checked",
    children: [
      { path: "/oa/leave/approval", title: "请假审批", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] },
      { path: "/oa/business-trip/approval", title: "出差审批", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] },
      { path: "/oa/outing/approval", title: "外出审批", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] },
      { path: "/oa/purchase/approval", title: "采购审批", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] },
      { path: "/oa/expense/approval", title: "经费审批", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] },
      { path: "/oa/overtime-approval", title: "加班审批", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] },
      { path: "/oa/loan-approval", title: "借支审批", roles: ["ADMIN", "DEPT_MANAGER", "TEAM_LEAD", "DIRECTOR", "GM"] }
    ]
  },
  {
    title: "报表中心",
    icon: "TrendCharts",
    children: [
      { path: "/oa/report/personal", title: "个人报表" },
      { path: "/oa/report/admin", title: "管理报表", roles: ["ADMIN", "DEPT_MANAGER"] }
    ]
  },
  {
    title: "薪资管理",
    icon: "Money",
    children: [
      { path: "/oa/salary-my", title: "我的薪资" },
      { path: "/oa/salary", title: "薪资管理", roles: ["ADMIN"] }
    ]
  },
  {
    title: "档案管理",
    icon: "FolderOpened",
    roles: ["ADMIN"]
  },
  {
    title: "系统管理",
    icon: "Setting",
    roles: ["ADMIN"],
    children: [
      { path: "/system/user", title: "员工管理" },
      { path: "/system/role", title: "角色管理" },
      { path: "/system/menu", title: "菜单权限" },
      { path: "/system/dept", title: "部门管理" },
      { path: "/system/dict", title: "字典管理" },
      { path: "/system/config", title: "参数配置" },
      { path: "/system/post", title: "岗位管理" }
    ]
  },
  {
    title: "系统监控",
    icon: "Monitor",
    roles: ["ADMIN"],
    children: [
      { path: "/monitor/online", title: "在线用户" },
      { path: "/monitor/logs/login", title: "登录日志" },
      { path: "/monitor/logs/operation", title: "操作日志" },
      { path: "/monitor/logs/system", title: "系统日志" }
    ]
  }
];
