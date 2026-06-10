import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, AlertRule, AlertLog } from "@/types/api";

export const getAlertRulePage = (params: PageParams & Partial<AlertRule>) =>
  request.get<unknown, ApiResponse<PageResult<AlertRule>>>("/api/alert/rule/page", { params });

export const addAlertRule = (data: Partial<AlertRule>) =>
  request.post<unknown, ApiResponse<void>>("/api/alert/rule", data);

export const updateAlertRule = (data: Partial<AlertRule>) =>
  request.put<unknown, ApiResponse<void>>("/api/alert/rule", data);

export const deleteAlertRule = (id: number) =>
  request.delete<unknown, ApiResponse<void>>(`/api/alert/rule/${id}`);

export const getAlertLogPage = (params: PageParams & Partial<AlertLog>) =>
  request.get<unknown, ApiResponse<PageResult<AlertLog>>>("/api/alert/log/page", { params });

export const handleAlert = (data: { id: number; handleRemark?: string }) =>
  request.post<unknown, ApiResponse<void>>(`/api/alert/log/handle/${data.id}`, {
    handleRemark: data.handleRemark || "已处理"
  });
