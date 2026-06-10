import request from "@/utils/request";
import type { ApiResponse, PageResult, OperationLog, LoginLog } from "@/types/api";

interface MonitorPageParams {
  page?: number;
  pageNum?: number;
  pageSize?: number;
}

const toPageParams = (params?: MonitorPageParams & Record<string, unknown>) => ({
  ...params,
  pageNum: params?.pageNum ?? params?.page ?? 1,
  pageSize: params?.pageSize ?? 10,
  page: undefined
});

export const getOnlineLogs = (params?: {
  page?: number;
  pageSize?: number;
}) => {
  return request.get<unknown, ApiResponse<PageResult<Record<string, unknown>>>>("/online-logs", { params: toPageParams(params) });
};

export const getLoginLogs = (params?: {
  page?: number;
  pageSize?: number;
  username?: string;
}) => {
  return request.get<unknown, ApiResponse<PageResult<LoginLog>>>("/login-logs", { params: toPageParams(params) });
};

export const getOperationLogs = (params?: {
  page?: number;
  pageSize?: number;
  module?: string;
}) => {
  return request.get<unknown, ApiResponse<PageResult<OperationLog>>>("/operation-logs", { params: toPageParams(params) });
};

export const getSystemLogs = (params?: {
  page?: number;
  pageSize?: number;
}) => {
  return request.get<unknown, ApiResponse<PageResult<Record<string, unknown>>>>("/system-logs", { params: toPageParams(params) });
};

export const getSystemLogsDetail = (id: number) => {
  return request.get<unknown, ApiResponse<Record<string, unknown>>>("/system-logs-detail", { params: { id } });
};

export const forceLogout = (id: number) => {
  return request.post<unknown, ApiResponse<void>>(`/online-logs/${id}/force-logout`);
};
