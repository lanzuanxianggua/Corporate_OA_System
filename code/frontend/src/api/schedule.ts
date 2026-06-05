import request from "@/utils/request";
import type { ApiResponse, PageResult, Schedule } from "@/types/api";

export const getSchedulePage = (params: {
  pageNum: number;
  pageSize: number;
  empId?: number;
}) => {
  return request.get<unknown, ApiResponse<PageResult<Schedule>>>("/api/schedule/page", { params });
};

export const addSchedule = (data: Partial<Schedule>) => {
  return request.post<unknown, ApiResponse<void>>("/api/schedule", data);
};

export const updateSchedule = (data: Partial<Schedule>) => {
  return request.put<unknown, ApiResponse<void>>("/api/schedule", data);
};

export const deleteSchedule = (id: number) => {
  return request.delete<unknown, ApiResponse<void>>(`/api/schedule/${id}`);
};
