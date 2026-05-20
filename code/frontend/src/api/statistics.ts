import request from "@/utils/request";

export const getDashboardStats = (period = "today", year?: number) => {
  return request.get<any, any>("/api/statistics/dashboard", {
    params: { period, year }
  });
};
