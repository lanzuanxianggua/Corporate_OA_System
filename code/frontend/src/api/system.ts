import request from "@/utils/request";
import type { ApiResponse, PageResult, UserVO, Role, Menu } from "@/types/api";

export const getUserPage = (params: {
  page: number;
  pageSize: number;
  username?: string;
  status?: number;
}) => {
  return request.post<unknown, ApiResponse<PageResult<UserVO>>>("/user", params);
};

export const getAllRoles = () => {
  return request.get<unknown, ApiResponse<Role[]>>("/list-all-role");
};

export const getRoles = () => {
  return request.get<unknown, ApiResponse<Role[]>>("/api/system/roles");
};

export const getEmpRoles = (empId: number) => {
  return request.get<unknown, ApiResponse<number[]>>("/emp-roles", { params: { empId } });
};

export const assignRoles = (empId: number, roleIds: number[]) => {
  return request.post<unknown, ApiResponse<void>>("/assign-roles", { empId, roleIds });
};

export const getRoleByUserId = (userId: number) => {
  return request.post<unknown, ApiResponse<number[]>>("/list-role-ids", { userId });
};

export const getRolePage = (params?: Partial<Role> & { page?: number; pageSize?: number }) => {
  return request.post<unknown, ApiResponse<PageResult<Role>>>("/role", params || {});
};

export const addRole = (data: Partial<Role>) => {
  return request.post<unknown, ApiResponse<void>>("/role/add", data);
};

export const updateRole = (data: Partial<Role>) => {
  return request.put<unknown, ApiResponse<void>>("/role/update", data);
};

export const deleteRole = (id: number) => {
  return request.delete<unknown, ApiResponse<void>>(`/role/${id}`);
};

export const getMenuList = () => {
  return request.post<unknown, ApiResponse<Menu[]>>("/menu", {});
};

export const getDeptList = () => {
  return request.post<unknown, ApiResponse<import("@/types/api").Dept[]>>("/dept", {});
};

export const getMine = () => {
  return request.get<unknown, ApiResponse<UserVO>>("/mine");
};

export const getMineLogs = (params?: { page?: number; pageSize?: number }) => {
  return request.get<unknown, ApiResponse<PageResult<import("@/types/api").LoginLog>>>("/mine-logs", { params });
};
