import request from "../utils/request";

export interface UserVO {
  id: number;
  username: string;
  nickname: string;
  phone: string;
  email: string;
  status: number;
  avatar?: string;
  createTime: string;
  dept?: {
    id: number;
    name: string;
  };
  roles?: Array<{
    id: number;
    name: string;
    code: string;
  }>;
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

export const getRoleByUserId = (userId: number) => {
  return request.post<any, any>("/list-role-ids", { userId });
};

export interface RoleVO {
  id: number;
  name: string;
  code: string;
  status: number;
  remark: string;
  createTime: string;
}

export const getRolePage = () => {
  return request.post<any, any>("/role", {});
};

export interface MenuVO {
  id: number;
  menuName: string;
  menuType: string;
  path: string;
  component: string;
  orderNum: number;
  icon: string;
  children?: MenuVO[];
}

export const getMenuList = () => {
  return request.post<any, any>("/menu", {});
};

export interface DeptVO {
  id: number;
  deptName: string;
  leader: string;
  phone: string;
  orderNum: number;
  status: number;
  createTime: string;
  children?: DeptVO[];
}

export const getDeptList = () => {
  return request.post<any, any>("/dept", {});
};