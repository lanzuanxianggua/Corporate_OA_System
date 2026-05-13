import request from "@/utils/request";

export const getOnlineLogs = (params?: {
  page?: number;
  pageSize?: number;
}) => {
  return request.post<any, any>("/online-logs", params || {});
};

export const getLoginLogs = (params?: {
  page?: number;
  pageSize?: number;
  username?: string;
}) => {
  return request.post<any, any>("/login-logs", params || {});
};

export const getOperationLogs = (params?: {
  page?: number;
  pageSize?: number;
  module?: string;
}) => {
  return request.post<any, any>("/operation-logs", params || {});
};

export const getSystemLogs = (params?: {
  page?: number;
  pageSize?: number;
}) => {
  return request.post<any, any>("/system-logs", params || {});
};

export const getSystemLogsDetail = (id: number) => {
  return request.post<any, any>("/system-logs-detail", { id });
};

export const forceLogout = (id: number) => {
  return request.post<any, any>("/online-logs", { id, action: "forceLogout" });
};
