import request from "@/utils/request";

export const getMyBalances = () =>
  request.get<any, any>("/api/leave-balance/my");

export const getBalancePage = (params: any) =>
  request.get<any, any>("/api/leave-balance/page", { params });

export const initBalance = (data: any) =>
  request.post("/api/leave-balance/init", data);
