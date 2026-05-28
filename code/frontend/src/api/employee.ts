import request from "@/utils/request";
import type { ApiResponse, PageResult, Employee } from "@/types/api";

export const getEmployeePage = (params: {
  pageNum: number;
  pageSize: number;
  empName?: string;
  deptId?: number;
}) => {
  return request.get<unknown, ApiResponse<PageResult<Employee>>>("/api/employee/page", { params });
};

export const getEmployeeById = (id: number) => {
  return request.get<unknown, ApiResponse<Employee>>(`/api/employee/${id}`);
};

export const addEmployee = (data: Partial<Employee>) => {
  return request.post<unknown, ApiResponse<void>>("/api/employee", data);
};

export const updateEmployee = (data: Partial<Employee>) => {
  return request.put<unknown, ApiResponse<void>>("/api/employee", data);
};

export const deleteEmployee = (id: number) => {
  return request.delete<unknown, ApiResponse<void>>(`/api/employee/${id}`);
};

export const updatePassword = (oldPwd: string, newPwd: string) =>
  request.post<unknown, ApiResponse<void>>("/api/auth/change-password", { oldPassword: oldPwd, newPassword: newPwd });
