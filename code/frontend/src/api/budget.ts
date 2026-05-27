import request from "@/utils/request";

export const getBudgetPage = (params: any) =>
  request.get<any, any>("/api/budget/page", { params });

export const addBudget = (data: any) =>
  request.post("/api/budget", data);

export const updateBudget = (data: any) =>
  request.put("/api/budget", data);

export const deleteBudget = (id: number) =>
  request.delete(`/api/budget/${id}`);

export const getBudgetByDeptMonth = (deptId: number, year: number, month: number) =>
  request.get<any, any>(`/api/budget/dept/${deptId}/month`, { params: { year, month } });
