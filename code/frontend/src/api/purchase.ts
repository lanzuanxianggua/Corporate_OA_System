import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, Purchase, ApproveDTO } from "@/types/api";

export const getPurchasePage = (params: PageParams & Partial<Purchase>) =>
  request.get<unknown, ApiResponse<PageResult<Purchase>>>("/api/purchase/page", { params });

export const submitPurchase = (data: Partial<Purchase>) =>
  request.post<unknown, ApiResponse<void>>("/api/purchase/submit", data);

export const approvePurchase = (data: ApproveDTO) =>
  request.post<unknown, ApiResponse<void>>("/api/purchase/approve", data);
