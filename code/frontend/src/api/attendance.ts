import request from "@/utils/request";
import type { ApiResponse, Attendance } from "@/types/api";

export const getTodayAttendance = () => {
  return request.get<unknown, ApiResponse<Attendance>>("/api/attendance/today");
};

export const getAttendanceHistory = (startDate: string, endDate: string) => {
  return request.get<unknown, ApiResponse<Attendance[]>>("/api/attendance/history", {
    params: { startDate, endDate }
  });
};

export const clockIn = () => {
  return request.post<unknown, ApiResponse<Attendance>>("/api/attendance/clock-in");
};

export const clockOut = () => {
  return request.post<unknown, ApiResponse<Attendance>>("/api/attendance/clock-out");
};

export const getAttendanceAdminPage = (params: {
  pageNum: number;
  pageSize: number;
  empName?: string;
  status?: number;
  startDate?: string;
  endDate?: string;
}) => {
  return request.get<unknown, ApiResponse<import("@/types/api").PageResult<Attendance>>>("/api/attendance/admin/page", { params });
};
