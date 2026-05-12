import { http } from "@/utils/http";

type Result = {
  code: number;
  message: string;
  data?: any;
};

/** 仪表盘统计 */
export const getDashboardStats = (params?: object) => {
  return http.request<Result>("get", "/api/statistics/dashboard", { params });
};
