import { http } from "@/utils/http";

type Result = {
  code: number;
  message: string;
  data?: any;
};

/** 获取部门树 */
export const getDeptTree = () => {
  return http.request<Result>("get", "/api/dept/tree");
};

/** 新增部门 */
export const addDept = (data?: object) => {
  return http.request<Result>("post", "/api/dept", { data });
};

/** 修改部门 */
export const updateDept = (data?: object) => {
  return http.request<Result>("put", "/api/dept", { data });
};

/** 删除部门 */
export const deleteDept = (id: number) => {
  return http.request<Result>("delete", `/api/dept/${id}`);
};
