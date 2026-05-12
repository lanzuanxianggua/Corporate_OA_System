import { http } from "@/utils/http";

type Result = {
  code: number;
  message: string;
  data?: any;
};

type PageResult = {
  code: number;
  message: string;
  data?: {
    list: Array<any>;
    total?: number;
    pageSize?: number;
    currentPage?: number;
  };
};

/** 获取员工列表 */
export const getEmployeePage = (params?: object) => {
  return http.request<PageResult>("get", "/api/employee/page", { params });
};

/** 获取所有员工 */
export const getAllEmployees = () => {
  return http.request<Result>("get", "/api/employee/list-all");
};

/** 获取员工详情 */
export const getEmployeeDetail = (id: number) => {
  return http.request<Result>("get", `/api/employee/${id}`);
};

/** 新增员工 */
export const addEmployee = (data?: object) => {
  return http.request<Result>("post", "/api/employee", { data });
};

/** 修改员工 */
export const updateEmployee = (data?: object) => {
  return http.request<Result>("put", "/api/employee", { data });
};

/** 删除员工 */
export const deleteEmployee = (id: number) => {
  return http.request<Result>("delete", `/api/employee/${id}`);
};
