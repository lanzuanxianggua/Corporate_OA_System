import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, LeaveBalance } from "@/types/api";

export const getMyBalances = () =>
  request.get<unknown, ApiResponse<LeaveBalance[]>>("/api/leave-balance/my");

export const getBalancePage = (params: PageParams & Partial<LeaveBalance>) =>
  request.get<unknown, ApiResponse<PageResult<LeaveBalance>>>("/api/leave-balance/page", { params });

export const initBalance = (data: Partial<LeaveBalance>) =>
  request.post<unknown, ApiResponse<void>>("/api/leave-balance/init", data);
