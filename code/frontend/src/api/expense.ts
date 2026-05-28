import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, Expense } from "@/types/api";

export const getExpensePage = (params: PageParams & Partial<Expense>) =>
  request.get<unknown, ApiResponse<PageResult<Expense>>>("/api/expense/page", { params });

export const submitExpense = (data: Partial<Expense>) =>
  request.post<unknown, ApiResponse<void>>("/api/expense/submit", data);

export const approveExpense = (data: { id: number; status: number; remark?: string }) =>
  request.post<unknown, ApiResponse<void>>("/api/expense/approve", data);
