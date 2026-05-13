import request from "@/utils/request";

export const getTodayAttendance = () => {
  return request.get<any, any>("/api/attendance/today");
};

export const getAttendancePage = (params: {
  pageNum: number;
  pageSize: number;
  status?: number;
}) => {
  return request.get<any, any>("/api/attendance/page", { params });
};

export const clockIn = () => {
  return request.post("/api/attendance/clock-in");
};

export const clockOut = () => {
  return request.post("/api/attendance/clock-out");
};
