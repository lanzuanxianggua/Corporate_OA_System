/**
 * Shared constants for the mobile app.
 * Values aligned with backend dict data and entity enums.
 */

/** Approval / workflow status map: status code -> label */
export const STATUS_MAP: Record<number, string> = {
  0: "待审批",
  1: "已通过",
  2: "已驳回",
  3: "已转办",
  4: "已撤回",
  5: "已退回"
};

/** Approval status -> CSS class for colored labels */
export const STATUS_CLASS_MAP: Record<number, string> = {
  0: "text-warning",
  1: "text-success",
  2: "text-danger",
  3: "text-primary",
  4: "text-warning",
  5: "text-gray"
};

/**
 * Leave type labels.
 * Backend uses: 1=年假, 2=事假, 3=病假, 4=婚假, 5=产假, 6=丧假
 * Index 0 is placeholder (not used by backend).
 */
export const LEAVE_TYPE_MAP: Record<number, string> = {
  1: "年假",
  2: "事假",
  3: "病假",
  4: "婚假",
  5: "产假",
  6: "丧假"
};

/** Leave type labels as array for picker component (index maps to leaveType value) */
export const LEAVE_TYPE_OPTIONS: string[] = [
  "",     // 0 - placeholder
  "年假", // 1
  "事假", // 2
  "病假", // 3
  "婚假", // 4
  "产假", // 5
  "丧假"  // 6
];

/** Expense category labels (aligned with backend OaExpense.category) */
export const EXPENSE_CATEGORY_MAP: Record<string, string> = {
  office: "办公费用",
  travel: "差旅费用",
  entertainment: "招待费用",
  training: "培训费用",
  purchase: "采购费用",
  other: "其他"
};

/** Expense category options for picker */
export const EXPENSE_CATEGORY_OPTIONS: string[] = [
  "办公费用",
  "差旅费用",
  "招待费用",
  "培训费用",
  "采购费用",
  "其他"
];

/** Business type labels for workflow tasks */
export const BUSINESS_TYPE_MAP: Record<string, string> = {
  leave: "请假",
  trip: "出差",
  outing: "外出",
  purchase: "采购",
  expense: "经费",
  overtime: "加班",
  loan: "借款",
  contract: "合同"
};

/** Attendance status labels */
export const ATTENDANCE_STATUS_MAP: Record<number, string> = {
  0: "正常",
  1: "迟到",
  2: "早退",
  3: "缺勤",
  4: "休息",
  5: "请假",
  6: "出差"
};

/** Attendance status CSS class */
export const ATTENDANCE_STATUS_CLASS: Record<number, string> = {
  0: "text-success",
  1: "text-warning",
  2: "text-warning",
  3: "text-danger",
  4: "text-gray",
  5: "text-primary",
  6: "text-primary"
};
