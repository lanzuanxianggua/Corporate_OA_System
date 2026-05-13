import request from "../utils/request";

export interface ScheduleVO {
  id?: number;
  empId: number;
  empName?: string;
  title: string;
  startTime: string;
  endTime: string;
  description?: string;
  createTime?: string;
}

export const getSchedulePage = (params: {
  pageNum: number;
  pageSize: number;
  empId?: number;
}) => {
  return request.get<any, any>("/schedule/page", { params });
};

export const addSchedule = (data: Partial<ScheduleVO>) => {
  return request.post("/schedule", data);
};

export const updateSchedule = (data: Partial<ScheduleVO>) => {
  return request.put("/schedule", data);
};

export const deleteSchedule = (id: number) => {
  return request.delete(`/schedule/${id}`);
};