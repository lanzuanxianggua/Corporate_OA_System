import { get, post } from "@/utils/request";

export const getTodayAttendance = () => get("/api/attendance/today");
export const getAttendanceHistory = (params: any) => get("/api/attendance/history?" + buildQuery(params));
export const clockIn = (data?: any) => post("/api/attendance/clock-in", data);
export const clockOut = (data?: any) => post("/api/attendance/clock-out", data);

function buildQuery(params: Record<string, any>): string {
  return Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== "")
    .map(([k, v]) => `${k}=${encodeURIComponent(v)}`)
    .join("&");
}
