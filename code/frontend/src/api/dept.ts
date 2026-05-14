import request from "@/utils/request";

export interface DeptVO {
  id?: number;
  deptName?: string;
  parentId?: number;
  sort?: number;
  leader?: string;
  phone?: string;
  status?: number;
  createTime?: string;
  children?: DeptVO[];
}

export const getDeptTree = () => {
  return request.get<any, any>("/api/dept/tree");
};

export const addDept = (data: Partial<DeptVO>) => {
  return request.post("/api/dept", data);
};

export const updateDept = (data: Partial<DeptVO>) => {
  return request.put("/api/dept", data);
};

export const deleteDept = (id: number) => {
  return request.delete(`/api/dept/${id}`);
};
