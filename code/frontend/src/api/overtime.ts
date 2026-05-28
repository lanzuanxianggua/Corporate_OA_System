import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, Overtime, ApproveDTO } from "@/types/api";

export const submitOvertime = (data: Partial<Overtime>) =>
  request.post<unknown, ApiResponse<void>>("/api/overtime/submit", data);

export const approveOvertime = (data: ApproveDTO) =>
  request.post<unknown, ApiResponse<void>>("/api/overtime/approve", data);

export const getOvertimePage = (params: PageParams & Partial<Overtime>) =>
  request.get<unknown, ApiResponse<PageResult<Overtime>>>("/api/overtime/page", { params });
