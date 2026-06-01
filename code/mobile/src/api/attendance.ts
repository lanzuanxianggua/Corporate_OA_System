import { get, post } from "@/utils/request";
import type { ApiResponse, Attendance } from "@/types/api";

export const getTodayAttendance = () => get<ApiResponse<Attendance>>("/api/attendance/today");

export const getAttendanceHistory = (params: { startDate: string; endDate: string }) =>
  get<ApiResponse<Attendance[]>>("/api/attendance/history", params);

export const clockIn = () => post<ApiResponse<null>>("/api/attendance/clock-in");

export const clockOut = () => post<ApiResponse<null>>("/api/attendance/clock-out");
