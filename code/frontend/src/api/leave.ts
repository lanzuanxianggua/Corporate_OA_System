import request from "@/utils/request";
import type { ApiResponse, PageResult, LeaveApply } from "@/types/api";

export const getLeavePage = (params: {
  pageNum: number;
  pageSize: number;
  empId?: number;
  status?: number;
}) => {
  return request.get<unknown, ApiResponse<PageResult<LeaveApply>>>("/api/leave/page", { params });
};

export const submitLeave = (data: Partial<LeaveApply>) => {
  return request.post<unknown, ApiResponse<void>>("/api/leave/submit", data);
};

export const approveLeave = (data: {
  id: number;
  status: number;
  remark?: string;
}) => {
  return request.post<unknown, ApiResponse<void>>("/api/leave/approve", data);
};
