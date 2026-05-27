import request from "@/utils/request";

export interface UserVO {
  id?: number;
  username?: string;
  nickname?: string;
  empName?: string;
  phone?: string;
  email?: string;
  status?: number;
  avatar?: string;
  createTime?: string;
  dept?: { id: number; name: string };
  roles?: Array<{ id: number; name: string; code: string }>;
}

export const getUserPage = (params: {
  page: number;
  pageSize: number;
  username?: string;
  status?: number;
}) => {
  return request.post<any, any>("/user", params);
};

export const getAllRoles = () => {
  return request.get<any, any>("/list-all-role");
};

export const getRoles = () => {
  return request.get<any, any>("/api/system/roles");
};

export const getEmpRoles = (empId: number) => {
  return request.get<any, any>("/emp-roles", { params: { empId } });
};

export const assignRoles = (empId: number, roleIds: number[]) => {
  return request.post<any, any>("/assign-roles", { empId, roleIds });
};

export const getRoleByUserId = (userId: number) => {
  return request.post<any, any>("/list-role-ids", { userId });
};

export interface RoleVO {
  id?: number;
  name?: string;
  code?: string;
  status?: number;
  remark?: string;
  createTime?: string;
}

export const getRolePage = (params?: Record<string, any>) => {
  return request.post<any, any>("/role", params || {});
};

export const addRole = (data: Record<string, any>) => {
  return request.post<any, any>("/role/add", data);
};

export const updateRole = (data: Record<string, any>) => {
  return request.put<any, any>("/role/update", data);
};

export const deleteRole = (id: number) => {
  return request.delete<any, any>(`/role/${id}`);
};

export interface MenuVO {
  id?: number;
  menuName?: string;
  menuType?: string;
  path?: string;
  component?: string;
  orderNum?: number;
  icon?: string;
  children?: MenuVO[];
}

export const getMenuList = () => {
  return request.post<any, any>("/menu", {});
};

export const getDeptList = () => {
  return request.post<any, any>("/dept", {});
};

export const getMine = () => {
  return request.get<any, any>("/mine");
};

export const getMineLogs = (params?: Record<string, any>) => {
  return request.get<any, any>("/mine-logs", { params });
};
