import { http } from "@/utils/http";

type Result = {
  code: number;
  message: string;
  data?: any;
};

type PageResult = {
  code: number;
  message: string;
  data?: {
    list: Array<any>;
    total?: number;
    pageSize?: number;
    currentPage?: number;
  };
};

/** 打卡 */
export const clockIn = () => {
  return http.request<Result>("post", "/api/attendance/clock-in");
};

/** 下班打卡 */
export const clockOut = () => {
  return http.request<Result>("post", "/api/attendance/clock-out");
};

/** 获取今日考勤状态 */
export const getTodayStatus = () => {
  return http.request<Result>("get", "/api/attendance/today-status");
};

/** 个人考勤记录分页 */
export const getAttendancePage = (params?: object) => {
  return http.request<PageResult>("get", "/api/attendance/page", { params });
};

/** 考勤统计 */
export const getAttendanceStats = (params?: object) => {
  return http.request<Result>("get", "/api/attendance/stats", { params });
};

/** 管理员-全员考勤查询 */
export const getAttendanceManage = (params?: object) => {
  return http.request<PageResult>("get", "/api/attendance/manage", { params });
};
