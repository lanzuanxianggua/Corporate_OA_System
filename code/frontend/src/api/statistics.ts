import request from "../utils/request";

export const getDashboardStats = (period = "today") => {
  return request.get<any, any>("/statistics/dashboard", { params: { period } });
};