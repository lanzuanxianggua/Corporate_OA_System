import { get, post } from "@/utils/request";

export const getTodayAttendance = () => get("/api/attendance/today");

export const getAttendanceHistory = (params: { startDate: string; endDate: string }) =>
  get("/api/attendance/history", params);

export const clockIn = () => post("/api/attendance/clock-in");

export const clockOut = () => post("/api/attendance/clock-out");
