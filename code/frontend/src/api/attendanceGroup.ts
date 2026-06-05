import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, AttendanceGroup } from "@/types/api";

export const getAttendanceGroupPage = (params: PageParams & Partial<AttendanceGroup>) =>
  request.get<unknown, ApiResponse<PageResult<AttendanceGroup>>>("/api/attendance-group/page", { params });

export const addAttendanceGroup = (data: Partial<AttendanceGroup>) =>
  request.post<unknown, ApiResponse<void>>("/api/attendance-group", data);

export const updateAttendanceGroup = (data: Partial<AttendanceGroup>) =>
  request.put<unknown, ApiResponse<void>>("/api/attendance-group", data);

export const deleteAttendanceGroup = (id: number) =>
  request.delete<unknown, ApiResponse<void>>(`/api/attendance-group/${id}`);

export const assignEmployees = (data: { groupId: number; empIds: number[] }) =>
  request.post<unknown, ApiResponse<void>>(`/api/attendance-group/${data.groupId}/employees`, { empIds: data.empIds });

export const removeEmployees = (data: { groupId: number; empIds: number[] }) =>
  request.delete<unknown, ApiResponse<void>>(`/api/attendance-group/${data.groupId}/employees`, { data: { empIds: data.empIds } });
