import request from "@/utils/request";

export const getTodayAttendance = () => {
  return request.get<any, any>("/api/attendance/today");
};

export const getAttendanceHistory = (startDate: string, endDate: string) => {
  return request.get<any, any>("/api/attendance/history", {
    params: { startDate, endDate }
  });
};

export const clockIn = () => {
  return request.post("/api/attendance/clock-in");
};

export const clockOut = () => {
  return request.post("/api/attendance/clock-out");
};

export const getAttendanceAdminPage = (params: {
  pageNum: number;
  pageSize: number;
  empName?: string;
  status?: number;
  startDate?: string;
  endDate?: string;
}) => {
  return request.get<any, any>("/api/attendance/admin/page", { params });
};
