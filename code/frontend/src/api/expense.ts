import request from "@/utils/request";

export const getExpensePage = (params: any) =>
  request.get<any, any>("/api/expense/page", { params });

export const submitExpense = (data: any) =>
  request.post("/api/expense/submit", data);

export const approveExpense = (data: any) =>
  request.post("/api/expense/approve", data);
