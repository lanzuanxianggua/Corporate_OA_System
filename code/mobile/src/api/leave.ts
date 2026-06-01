import { get, post } from "@/utils/request";
import type { ApiResponse, PageResult, LeaveApply, LeaveBalance, SubmitLeaveParams } from "@/types/api";

export const getLeavePage = (params: any) => get<ApiResponse<PageResult<LeaveApply>>>("/api/leave/page", params);
export const submitLeave = (data: SubmitLeaveParams | any) => post<ApiResponse<null>>("/api/leave/submit", data);
export const approveLeave = (data: any) => post<ApiResponse<null>>("/api/leave/approve", data);
export const getMyBalances = () => get<ApiResponse<LeaveBalance[]>>("/api/leave-balance/my");
