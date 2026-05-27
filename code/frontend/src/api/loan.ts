import request from "@/utils/request";

export const submitLoan = (data: any) =>
  request.post("/api/loan/submit", data);

export const approveLoan = (data: any) =>
  request.post("/api/loan/approve", data);

export const getLoanPage = (params: any) =>
  request.get<any, any>("/api/loan/page", { params });

export const addRepayment = (data: any) =>
  request.post("/api/loan/repayment", data);
