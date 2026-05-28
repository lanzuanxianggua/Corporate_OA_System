/**
 * Shared constants for the mobile app.
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

/** Leave type labels, indexed by leaveType integer */
export const LEAVE_TYPE_MAP: string[] = [
  "事假",
  "病假",
  "年假",
  "婚假",
  "产假",
  "丧假"
];
