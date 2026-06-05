import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, Loan, LoanRepayment, ApproveDTO } from "@/types/api";

export const submitLoan = (data: Partial<Loan>) =>
  request.post<unknown, ApiResponse<void>>("/api/loan/submit", data);

export const approveLoan = (data: ApproveDTO) =>
  request.post<unknown, ApiResponse<void>>("/api/loan/approve", data);

export const getLoanPage = (params: PageParams & Partial<Loan>) =>
  request.get<unknown, ApiResponse<PageResult<Loan>>>("/api/loan/page", { params });

export const addRepayment = (data: Partial<LoanRepayment>) =>
  request.post<unknown, ApiResponse<void>>("/api/loan/repayment", data);
