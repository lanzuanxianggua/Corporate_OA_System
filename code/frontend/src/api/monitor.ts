import request from "@/utils/request";
import type { ApiResponse, PageResult, OperationLog, LoginLog } from "@/types/api";

export const getOnlineLogs = (params?: {
  page?: number;
  pageSize?: number;
}) => {
  return request.post<unknown, ApiResponse<PageResult<Record<string, unknown>>>>("/online-logs", params || {});
};

export const getLoginLogs = (params?: {
  page?: number;
  pageSize?: number;
  username?: string;
}) => {
  return request.post<unknown, ApiResponse<PageResult<LoginLog>>>("/login-logs", params || {});
};

export const getOperationLogs = (params?: {
  page?: number;
  pageSize?: number;
  module?: string;
}) => {
  return request.post<unknown, ApiResponse<PageResult<OperationLog>>>("/operation-logs", params || {});
};

export const getSystemLogs = (params?: {
  page?: number;
  pageSize?: number;
}) => {
  return request.post<unknown, ApiResponse<PageResult<Record<string, unknown>>>>("/system-logs", params || {});
};

export const getSystemLogsDetail = (id: number) => {
  return request.post<unknown, ApiResponse<Record<string, unknown>>>("/system-logs-detail", { id });
};

export const forceLogout = (id: number) => {
  return request.post<unknown, ApiResponse<void>>("/online-logs", { id, action: "forceLogout" });
};
