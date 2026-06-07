import { apiGet, apiPost } from "@/utils/http";
import type { PageResult } from "@/types/api";

// ──────────────────────────────────────────────────────────────────────────
// v2 type definitions (matched to backend `cn.oa.hr.leave.*` v2 DTO/VO)
// Backend now uses String enums for leaveType / status.
// ──────────────────────────────────────────────────────────────────────────

export interface HrLeaveVO {
  id?: number;
  empId?: number;
  empName?: string;
  deptName?: string;
  /** ANNUAL / SICK / PERSONAL / MARRIAGE / MATERNITY */
  leaveType?: string;
  startDate?: string;
  endDate?: string;
  totalDays?: number;
  reason?: string;
  /** PENDING / APPROVED / REJECTED / CANCELLED */
  status?: string;
  wfInstanceId?: number;
  createTime?: string;
}

export interface HrLeaveCreateDTO {
  leaveType: string;
  startDate: string;
  endDate: string;
  reason?: string;
}

export interface HrLeaveQueryDTO {
  pageNum: number;
  pageSize: number;
  status?: string;
  leaveType?: string;
}

export interface HrLeaveBalance {
  id?: number;
  empId?: number;
  /** ANNUAL / SICK / PERSONAL / MARRIAGE / MATERNITY */
  leaveType?: string;
  year?: number;
  totalDays?: number;
  usedDays?: number;
  /** 已冻结(审批中)天数 */
  frozenDays?: number;
  remainingDays?: number;
  /** ACTIVE / FROZEN / DEPLETED */
  status?: string;
}

/** Detail endpoint returns Map<String, Object> in backend; surface stable fields. */
export interface HrLeaveDetail {
  id?: number;
  empId?: number;
  empName?: string;
  deptName?: string;
  leaveType?: string;
  startDate?: string;
  endDate?: string;
  totalDays?: number;
  reason?: string;
  status?: string;
  wfInstanceId?: number;
  createTime?: string;
}

// ── v2 lookups (String keys) ──────────────────────────────────────────────

export const LEAVE_TYPE_MAP_V2: Record<string, string> = {
  ANNUAL: "年假",
  SICK: "病假",
  PERSONAL: "事假",
  MARRIAGE: "婚假",
  MATERNITY: "产假"
};

export const LEAVE_TYPE_OPTIONS = Object.entries(LEAVE_TYPE_MAP_V2).map(([value, label]) => ({
  value,
  label
}));

export const LEAVE_STATUS_MAP_V2: Record<string, string> = {
  PENDING: "待审批",
  APPROVED: "已通过",
  REJECTED: "已驳回",
  CANCELLED: "已撤回"
};

export const LEAVE_STATUS_TAG_V2: Record<string, "warning" | "success" | "danger" | "info"> = {
  PENDING: "warning",
  APPROVED: "success",
  REJECTED: "danger",
  CANCELLED: "info"
};

export const LEAVE_STATUS_OPTIONS = Object.entries(LEAVE_STATUS_MAP_V2).map(([value, label]) => ({
  value,
  label
}));

// ──────────────────────────────────────────────────────────────────────────
// v1 backward-compat re-exports (number-typed enums + interfaces)
// Consumed by /views/hr/leave/index.vue which is the legacy v1 module.
// ──────────────────────────────────────────────────────────────────────────

export const LEAVE_TYPE_MAP: Record<number, string> = {
  1: "年假",
  2: "事假",
  3: "病假",
  4: "婚假",
  5: "产假",
  6: "丧假",
  7: "调休"
};

export const LEAVE_STATUS_MAP: Record<number, string> = {
  0: "待审批",
  1: "已通过",
  2: "已驳回",
  3: "已撤回"
};

export const LEAVE_STATUS_TAG: Record<number, "warning" | "success" | "danger" | "info"> = {
  0: "warning",
  1: "success",
  2: "danger",
  3: "info"
};

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

// ── v1 API client (legacy) ────────────────────────────────────────────────

export const leaveApi = {
  listMy(params: { pageNum: number; pageSize: number; leaveType?: number; status?: number }) {
    return apiGet<LeavePageResult>("/api/v1/hr-leave/leaves/mine", params);
  },
  getDetail(id: number) {
    return apiGet<LeaveVO>(`/api/v1/hr-leave/leaves/${id}`);
  },
  create(data: LeaveCreateDTO) {
    return apiPost<number>("/api/v1/hr-leave/leaves", data);
  },
  revoke(id: number) {
    return apiPost<void>(`/api/v1/hr-leave/leaves/${id}/actions/revoke`, {});
  },
  getBalances() {
    return apiGet<LeaveBalanceVO[]>("/api/v1/hr-leave/leaves/balances/me");
  }
};

// ── v2 API client (preferred) ─────────────────────────────────────────────

export const hrLeaveApi = {
  /**
   * GET /api/v1/hr-leave/leaves/mine — 我的请假列表(分页)
   * Permission: hr-leave:leave:list
   */
  listMy(params: HrLeaveQueryDTO) {
    return apiGet<PageResult<HrLeaveVO>>("/api/v1/hr-leave/leaves/mine", params);
  },

  /**
   * GET /api/v1/hr-leave/leaves/{id} — 请假详情
   * Permission: hr-leave:leave:list
   */
  getDetail(id: number) {
    return apiGet<HrLeaveDetail>(`/api/v1/hr-leave/leaves/${id}`);
  },

  /**
   * POST /api/v1/hr-leave/leaves — 提交请假
   * Returns the created leave id. Permission: hr-leave:leave:create
   */
  create(data: HrLeaveCreateDTO) {
    return apiPost<number>("/api/v1/hr-leave/leaves", data);
  },

  /**
   * POST /api/v1/hr-leave/leaves/{id}/actions/revoke — 撤回请假(仅 PENDING)
   * Permission: hr-leave:leave:create
   */
  revoke(id: number) {
    return apiPost<void>(`/api/v1/hr-leave/leaves/${id}/actions/revoke`, {});
  },

  /**
   * GET /api/v1/hr-leave/balances/me — 我的假期余额(v2 完整版,含 year/frozenDays)
   * Returns HrLeaveBalance entity list. Permission: hr-leave:leave-balance:view
   */
  getBalances() {
    return apiGet<HrLeaveBalance[]>("/api/v1/hr-leave/balances/me");
  }
};
