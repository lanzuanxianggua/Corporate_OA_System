import { get } from "@/utils/request";

export const getEmployeeById = (id: number) => get(`/api/employee/${id}`);

export const getEmployeePage = (params: { pageNum: number; pageSize: number; empName?: string }) =>
  get("/api/employee/page", params);
