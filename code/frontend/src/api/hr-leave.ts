import { apiGet, apiPost } from "@/utils/http";

// ── Type definitions ─────────────────────────────────────────────────────────

export interface LeaveVO {
  id: number;
  empId: number;
  empName: string;
  deptName: string;
  leaveType: number;
  startDate: string;
  endDate: string;
  days: number;
  reason: string;
  status: number;
  createTime: string;
  updateTime: string;
}

export interface LeaveCreateDTO {
  leaveType: number;
  startDate: string;
  endDate: string;
  reason: string;
}

export interface LeaveBalanceVO {
  leaveType: number;
  totalDays: number;
  usedDays: number;
  remainDays: number;
}

export interface LeavePageResult {
  list: LeaveVO[];
  total: number;
  pageNum: number;
  pageSize: number;
}

// ── Leave type / status lookups ──────────────────────────────────────────────

export const LEAVE_TYPE_MAP: Record<number, string> = {
  1: "年假",
  2: "事假",
  3: "病假",
  4: "婚假",
  5: "产假",
  6: "丧假",
  7: "调休",
};

export const LEAVE_STATUS_MAP: Record<number, string> = {
  0: "待审批",
  1: "已通过",
  2: "已驳回",
  3: "已撤回",
};

export const LEAVE_STATUS_TAG: Record<number, "warning" | "success" | "danger" | "info"> = {
  0: "warning",
  1: "success",
  2: "danger",
  3: "info",
};

// ── API methods ──────────────────────────────────────────────────────────────

export const leaveApi = {
  /** GET /api/v1/hr/leaves/mine — 我的请假列表（分页） */
  listMy(params: { pageNum: number; pageSize: number; leaveType?: number; status?: number }) {
    return apiGet<LeavePageResult>("/api/v1/hr/leaves/mine", params);
  },

  /** GET /api/v1/hr/leaves/{id} — 请假详情 */
  getDetail(id: number) {
    return apiGet<LeaveVO>(`/api/v1/hr/leaves/${id}`);
  },

  /** POST /api/v1/hr/leaves — 新建请假 */
  create(data: LeaveCreateDTO) {
    return apiPost<number>("/api/v1/hr/leaves", data);
  },

  /** POST /api/v1/hr/leaves/{id}/actions/revoke — 撤回请假（仅 PENDING） */
  revoke(id: number) {
    return apiPost<void>(`/api/v1/hr/leaves/${id}/actions/revoke`, {});
  },

  /** GET /api/v1/hr/leaves/balances/me — 我的假期余额 */
  getBalances() {
    return apiGet<LeaveBalanceVO[]>("/api/v1/hr/leaves/balances/me");
  },
};
