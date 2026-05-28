import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, Budget } from "@/types/api";

export const getBudgetPage = (params: PageParams & Partial<Budget>) =>
  request.get<unknown, ApiResponse<PageResult<Budget>>>("/api/budget/page", { params });

export const addBudget = (data: Partial<Budget>) =>
  request.post<unknown, ApiResponse<void>>("/api/budget", data);

export const updateBudget = (data: Partial<Budget>) =>
  request.put<unknown, ApiResponse<void>>("/api/budget", data);

export const deleteBudget = (id: number) =>
  request.delete<unknown, ApiResponse<void>>(`/api/budget/${id}`);

export const getBudgetByDeptMonth = (deptId: number, year: number, month: number) =>
  request.get<unknown, ApiResponse<Budget>>(`/api/budget/dept/${deptId}/month`, { params: { year, month } });
