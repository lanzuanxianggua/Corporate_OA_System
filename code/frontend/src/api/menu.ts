import request from "@/utils/request";
import type { ApiResponse, Menu } from "@/types/api";

export const getMenuTree = () =>
  request.get<unknown, ApiResponse<Menu[]>>("/api/menu/tree");

export const getMenuByRole = (roleId: number) =>
  request.get<unknown, ApiResponse<Menu[]>>(`/api/menu/role/${roleId}`);

export const addMenu = (data: Partial<Menu>) =>
  request.post<unknown, ApiResponse<void>>("/api/menu", data);

export const updateMenu = (data: Partial<Menu>) =>
  request.put<unknown, ApiResponse<void>>("/api/menu", data);

export const deleteMenu = (id: number) =>
  request.delete<unknown, ApiResponse<void>>(`/api/menu/${id}`);

export const assignRoleMenus = (data: { roleId: number; menuIds: number[] }) =>
  request.put<unknown, ApiResponse<void>>(`/api/menu/role/${data.roleId}`, data.menuIds);
