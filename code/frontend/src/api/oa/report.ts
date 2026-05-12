import { http } from "@/utils/http";

type Result = {
  code: number;
  message: string;
  data?: any;
};

/** 个人-考勤汇总 */
export const getPersonalAttendanceSummary = (params?: object) => {
  return http.request<Result>("get", "/api/report/personal/attendance-summary", { params });
};

/** 个人-出勤趋势 */
export const getPersonalAttendanceTrend = (params?: object) => {
  return http.request<Result>("get", "/api/report/personal/attendance-trend", { params });
};

/** 个人-请假统计 */
export const getPersonalLeaveSummary = (params?: object) => {
  return http.request<Result>("get", "/api/report/personal/leave-summary", { params });
};

/** 个人-月度对比 */
export const getPersonalMonthlyCompare = (params?: object) => {
  return http.request<Result>("get", "/api/report/personal/monthly-compare", { params });
};

/** 管理员-全员考勤汇总 */
export const getAdminAttendanceSummary = (params?: object) => {
  return http.request<Result>("get", "/api/report/admin/attendance-summary", { params });
};

/** 管理员-部门出勤对比 */
export const getAdminDeptCompare = (params?: object) => {
  return http.request<Result>("get", "/api/report/admin/dept-compare", { params });
};

/** 管理员-全员出勤趋势 */
export const getAdminAttendanceTrend = (params?: object) => {
  return http.request<Result>("get", "/api/report/admin/attendance-trend", { params });
};

/** 管理员-请假分析 */
export const getAdminLeaveAnalysis = (params?: object) => {
  return http.request<Result>("get", "/api/report/admin/leave-analysis", { params });
};

/** 管理员-员工排名 */
export const getAdminEmployeeRanking = (params?: object) => {
  return http.request<Result>("get", "/api/report/admin/employee-ranking", { params });
};

/** 管理员-今日概览 */
export const getAdminTodayOverview = () => {
  return http.request<Result>("get", "/api/report/admin/today-overview");
};
