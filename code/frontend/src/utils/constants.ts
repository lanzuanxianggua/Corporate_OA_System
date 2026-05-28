export const LEAVE_TYPE_MAP: Record<number, string> = {
  1: "事假",
  2: "病假",
  3: "年假",
  4: "婚假",
  5: "调休",
  6: "产假",
  7: "丧假"
};

// Consistent with format.ts: 0=pending, 1=approved, 2=rejected
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

export const EXPENSE_CATEGORY_MAP: Record<number, string> = {
  1: "差旅费",
  2: "办公用品",
  3: "招待费",
  4: "其他"
};
