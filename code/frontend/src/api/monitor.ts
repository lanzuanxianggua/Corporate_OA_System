import request from "../utils/request";

export interface OnlineUserVO {
  id: number;
  username: string;
  ip: string;
  dept?: string;
  browser: string;
  loginTime: string;
}

export interface LoginLogVO {
  id: number;
  username: string;
  ip: string;
  address: string;
  browser: string;
  system: string;
  status: string;
  behavior: string;
  loginTime: string;
}

export interface OperationLogVO {
  id: number;
  username: string;
  ip: string;
  address: string;
  system: string;
  browser: string;
  status: string;
  summary: string;
  module: string;
  operatingTime: string;
}

export const getOnlineLogs = (params?: { page?: number; pageSize?: number }) => {
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

export const forceLogout = (id: number) => {
  return request.delete<any, any>(`/online-logs/${id}`);
};