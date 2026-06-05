import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, Outing, ApproveDTO } from "@/types/api";

export const getOutingPage = (params: PageParams & Partial<Outing>) =>
  request.get<unknown, ApiResponse<PageResult<Outing>>>("/api/outing/page", { params });

export const submitOuting = (data: Partial<Outing>) =>
  request.post<unknown, ApiResponse<void>>("/api/outing/submit", data);

export const approveOuting = (data: ApproveDTO) =>
  request.post<unknown, ApiResponse<void>>("/api/outing/approve", data);
