/**
 * Leave type mapping matching backend LeaveApplyController.LEAVE_TYPE_TEXT.
 * Index: 1=年假, 2=事假, 3=病假, 4=婚假, 5=产假, 6=丧假, 7=调休
 */
export const LEAVE_TYPE_MAP: Record<number, string> = {
  1: "年假",
  2: "事假",
  3: "病假",
  4: "婚假",
  5: "产假",
  6: "丧假",
  7: "调休"
};

/**
 * Approval status mapping matching backend workflow convention.
 * 0=pending, 1=approved, 2=rejected, 3=canceled/withdrawn
 */
export const STATUS_MAP: Record<number, string> = {
  0: "待审批",
  1: "已通过",
  2: "已拒绝",
  3: "已撤回"
};

export const STATUS_TAG_TYPE: Record<number, string> = {
  0: "warning",
  1: "success",
  2: "danger",
  3: "info"
};

/**
 * Attendance status mapping matching backend AttendanceController.STATUS_TEXT.
 * 0=正常, 1=迟到, 2=早退, 3=缺勤, 4=休息, 5=请假, 6=出差
 */
export const ATTENDANCE_STATUS_MAP: Record<number, string> = {
  0: "正常",
  1: "迟到",
  2: "早退",
  3: "缺勤",
  4: "休息",
  5: "请假",
  6: "出差"
};

export const ATTENDANCE_STATUS_TAG_TYPE: Record<number, string> = {
  0: "success",
  1: "warning",
  2: "warning",
  3: "danger",
  4: "info",
  5: "info",
  6: ""
};

/**
 * Expense categories matching backend OaExpense.category field (string type).
 */
export const EXPENSE_CATEGORY_MAP: Record<string, string> = {
  travel: "差旅费",
  office: "办公用品",
  entertainment: "招待费",
  other: "其他"
};

/**
 * Workflow task status matching backend WfTask.
 * 0=pending, 1=approved, 2=rejected, 3=transferred, 4=canceled, 5=returned
 */
export const TASK_STATUS_MAP: Record<string, string> = {
  "0": "待处理",
  "1": "已通过",
  "2": "已拒绝",
  "3": "已转办",
  "4": "已取消",
  "5": "已退回"
};

export const TASK_STATUS_TAG_TYPE: Record<string, string> = {
  "0": "warning",
  "1": "success",
  "2": "danger",
  "3": "info",
  "4": "info",
  "5": "info"
};

/**
 * Process definition status.
 * 0=inactive/disabled, 1=active
 */
export const PROCESS_STATUS_MAP: Record<string, string> = {
  "0": "停用",
  "1": "启用"
};

/**
 * Process instance status matching backend WfProcessInstance.
 * 0=running, 1=approved, 2=rejected, 3=canceled
 */
export const INSTANCE_STATUS_MAP: Record<string, string> = {
  "0": "进行中",
  "1": "已通过",
  "2": "已拒绝",
  "3": "已取消"
};

/**
 * Asset borrow status matching backend OaAssetBorrow.
 * 0=borrowed, 1=returned
 */
export const ASSET_BORROW_STATUS_MAP: Record<string, string> = {
  "0": "借出中",
  "1": "已归还"
};
