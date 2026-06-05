import request from "@/utils/request";
import type { ApiResponse } from "@/types/api";

export const getDashboardStats = (period = "today", year?: number) => {
  return request.get<unknown, ApiResponse<Record<string, unknown>>>("/api/statistics/dashboard", {
    params: { period, year }
  });
};
