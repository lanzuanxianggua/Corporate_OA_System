import request from "@/utils/request";
import type { ApiResponse } from "@/types/api";

export const getPersonalAttendanceSummary = (month: string, period = "month") => {
  return request.get<unknown, ApiResponse<Record<string, unknown>>>("/api/report/personal/attendance-summary", {
    params: { month, period }
  });
};

export const getPersonalAttendanceTrend = (months = 6, period = "month") => {
  return request.get<unknown, ApiResponse<Record<string, unknown>[]>>("/api/report/personal/attendance-trend", {
    params: { months, period }
  });
};

export const getPersonalLeaveSummary = (month: string) => {
  return request.get<unknown, ApiResponse<Record<string, unknown>>>("/api/report/personal/leave-summary", {
    params: { month }
  });
};

export const getPersonalMonthlyCompare = (month: string) => {
  return request.get<unknown, ApiResponse<Record<string, unknown>>>("/api/report/personal/monthly-compare", {
    params: { month }
  });
};

export const getAdminAttendanceSummary = (month: string) => {
  return request.get<unknown, ApiResponse<Record<string, unknown>>>("/api/report/admin/attendance-summary", {
    params: { month }
  });
};

export const getAdminDeptCompare = (month: string) => {
  return request.get<unknown, ApiResponse<Record<string, unknown>[]>>("/api/report/admin/dept-compare", {
    params: { month }
  });
};

export const getAdminAttendanceTrend = (month: string, months = 12) => {
  return request.get<unknown, ApiResponse<Record<string, unknown>[]>>("/api/report/admin/attendance-trend", {
    params: { month, months }
  });
};

export const getAdminLeaveAnalysis = (month: string) => {
  return request.get<unknown, ApiResponse<Record<string, unknown>>>("/api/report/admin/leave-analysis", {
    params: { month }
  });
};

export const getAdminEmployeeRanking = (month: string, type = "best") => {
  return request.get<unknown, ApiResponse<Record<string, unknown>[]>>("/api/report/admin/employee-ranking", {
    params: { month, type }
  });
};

export const getAdminTodayOverview = () => {
  return request.get<unknown, ApiResponse<Record<string, unknown>>>("/api/report/admin/today-overview");
};
