import { get, post } from "@/utils/request";
import type { ApiResponse, PageResult, BusinessTrip, Outing, Overtime, Purchase, Expense, Loan } from "@/types/api";

// Business Trip
export const getBusinessTripPage = (params: any) => get<ApiResponse<PageResult<BusinessTrip>>>("/api/business-trip/page", params);
export const submitBusinessTrip = (data: any) => post<ApiResponse<null>>("/api/business-trip/submit", data);

// Outing
export const getOutingPage = (params: any) => get<ApiResponse<PageResult<Outing>>>("/api/outing/page", params);
export const submitOuting = (data: any) => post<ApiResponse<null>>("/api/outing/submit", data);

// Overtime
export const getOvertimePage = (params: any) => get<ApiResponse<PageResult<Overtime>>>("/api/overtime/page", params);
export const submitOvertime = (data: any) => post<ApiResponse<null>>("/api/overtime/submit", data);

// Purchase
export const getPurchasePage = (params: any) => get<ApiResponse<PageResult<Purchase>>>("/api/purchase/page", params);
export const submitPurchase = (data: any) => post<ApiResponse<null>>("/api/purchase/submit", data);

// Expense
export const getExpensePage = (params: any) => get<ApiResponse<PageResult<Expense>>>("/api/expense/page", params);
export const submitExpense = (data: any) => post<ApiResponse<null>>("/api/expense/submit", data);

// Loan
export const getLoanPage = (params: any) => get<ApiResponse<PageResult<Loan>>>("/api/loan/page", params);
export const submitLoan = (data: any) => post<ApiResponse<null>>("/api/loan/submit", data);
