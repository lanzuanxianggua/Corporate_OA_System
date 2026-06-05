import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, BusinessTrip, ApproveDTO } from "@/types/api";

export const getBusinessTripPage = (params: PageParams & Partial<BusinessTrip>) =>
  request.get<unknown, ApiResponse<PageResult<BusinessTrip>>>("/api/business-trip/page", { params });

export const submitBusinessTrip = (data: Partial<BusinessTrip>) =>
  request.post<unknown, ApiResponse<void>>("/api/business-trip/submit", data);

export const approveBusinessTrip = (data: ApproveDTO) =>
  request.post<unknown, ApiResponse<void>>("/api/business-trip/approve", data);
