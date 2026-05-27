import request from "@/utils/request";

export const getMenuTree = () =>
  request.get<any, any>("/api/menu/tree");

export const getMenuByRole = (roleId: number) =>
  request.get<any, any>(`/api/menu/role/${roleId}`);

export const addMenu = (data: any) =>
  request.post("/api/menu", data);

export const updateMenu = (data: any) =>
  request.put("/api/menu", data);

export const deleteMenu = (id: number) =>
  request.delete(`/api/menu/${id}`);

export const assignRoleMenus = (data: { roleId: number; menuIds: number[] }) =>
  request.put(`/api/menu/role/${data.roleId}`, data.menuIds);
