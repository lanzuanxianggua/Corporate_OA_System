import request from "@/utils/request";

export interface EmployeeVO {
  id?: number;
  username?: string;
  password?: string;
  empName?: string;
  phone?: string;
  email?: string;
  deptId?: number;
  deptName?: string;
  status?: number;
  roles?: Array<{ id: number; name: string; code: string }>;
  createTime?: string;
}

export const getEmployeePage = (params: {
  pageNum: number;
  pageSize: number;
  empName?: string;
  deptId?: number;
}) => {
  return request.get<any, any>("/api/employee/page", { params });
};

export const getEmployeeById = (id: number) => {
  return request.get<any, any>(`/api/employee/${id}`);
};

export const addEmployee = (data: Partial<EmployeeVO>) => {
  return request.post("/api/employee", data);
};

export const updateEmployee = (data: Partial<EmployeeVO>) => {
  return request.put("/api/employee", data);
};

export const deleteEmployee = (id: number) => {
  return request.delete(`/api/employee/${id}`);
};

export const updatePassword = (
  empId: number,
  oldPwd: string,
  newPwd: string
) => {
  return request.put("/api/employee/password", null, {
    params: { empId, oldPwd, newPwd }
  });
};
