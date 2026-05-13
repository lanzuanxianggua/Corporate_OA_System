import request from "../utils/request";

export interface AttendanceVO {
  id?: number;
  empId: number;
  clockInTime?: string;
  clockOutTime?: string;
  status?: string;
  workHours?: number;
}

export const getTodayAttendance = () => {
  return request.get<any, any>("/attendance/today");
};

export const clockIn = () => {
  return request.post("/attendance/clock-in");
};

export const clockOut = () => {
  return request.post("/attendance/clock-out");
};