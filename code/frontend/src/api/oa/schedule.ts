import { http } from "@/utils/http";

type Result = {
  code: number;
  message: string;
  data?: any;
};

/** 获取日程列表 */
export const getScheduleList = (params?: object) => {
  return http.request<Result>("get", "/api/schedule/list", { params });
};

/** 新增日程 */
export const addSchedule = (data?: object) => {
  return http.request<Result>("post", "/api/schedule", { data });
};

/** 修改日程 */
export const updateSchedule = (data?: object) => {
  return http.request<Result>("put", "/api/schedule", { data });
};

/** 删除日程 */
export const deleteSchedule = (id: number) => {
  return http.request<Result>("delete", `/api/schedule/${id}`);
};

/** 按日期范围查询 */
export const getScheduleByDateRange = (params?: object) => {
  return http.request<Result>("get", "/api/schedule/date-range", { params });
};

/** 管理员-查看部门日程 */
export const getDepartmentSchedule = (deptId: number, params?: object) => {
  return http.request<Result>("get", `/api/schedule/department/${deptId}`, { params });
};
