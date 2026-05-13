import request from "../utils/request";

export const getPersonalAttendanceSummary = (month: string) => {
  return request.get<any, any>("/report/personal/attendance-summary", {
    params: { month }
  });
};

export const getPersonalAttendanceTrend = (months = 6) => {
  return request.get<any, any>("/report/personal/attendance-trend", {
    params: { months }
  });
};

export const getPersonalLeaveSummary = (month: string) => {
  return request.get<any, any>("/report/personal/leave-summary", {
    params: { month }
  });
};

export const getPersonalMonthlyCompare = (month: string) => {
  return request.get<any, any>("/report/personal/monthly-compare", {
    params: { month }
  });
};

export const getAdminAttendanceSummary = (month: string) => {
  return request.get<any, any>("/report/admin/attendance-summary", {
    params: { month }
  });
};

export const getAdminDeptCompare = (month: string) => {
  return request.get<any, any>("/report/admin/dept-compare", {
    params: { month }
  });
};

export const getAdminAttendanceTrend = (month: string, months = 12) => {
  return request.get<any, any>("/report/admin/attendance-trend", {
    params: { month, months }
  });
};

export const getAdminLeaveAnalysis = (month: string) => {
  return request.get<any, any>("/report/admin/leave-analysis", {
    params: { month }
  });
};

export const getAdminEmployeeRanking = (month: string, type = "best") => {
  return request.get<any, any>("/report/admin/employee-ranking", {
    params: { month, type }
  });
};