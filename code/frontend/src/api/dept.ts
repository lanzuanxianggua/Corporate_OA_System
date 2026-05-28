import request from "@/utils/request";
import type { ApiResponse, Dept } from "@/types/api";

export const getDeptTree = () => {
  return request.get<unknown, ApiResponse<Dept[]>>("/api/dept/tree");
};

export const addDept = (data: Partial<Dept>) => {
  return request.post<unknown, ApiResponse<void>>("/api/dept", data);
};

export const updateDept = (data: Partial<Dept>) => {
  return request.put<unknown, ApiResponse<void>>("/api/dept", data);
};

export const deleteDept = (id: number) => {
  return request.delete<unknown, ApiResponse<void>>(`/api/dept/${id}`);
};
