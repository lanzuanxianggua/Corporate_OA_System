import { del, get, post } from "@/utils/request";
import type { ApiResponse, PageResult, Schedule } from "@/types/api";

export const getSchedulePage = (params: any) => get<ApiResponse<PageResult<Schedule>>>("/api/schedule/page", params);
export const addSchedule = (data: any) => post<ApiResponse<null>>("/api/schedule", data);
export const updateSchedule = (data: any) => post<ApiResponse<null>>("/api/schedule", data);
export const deleteSchedule = (id: number) => del<ApiResponse<null>>(`/api/schedule/${id}`);
