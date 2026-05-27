import { get, post } from "@/utils/request";

// Business Trip
export const getBusinessTripPage = (params: any) => get("/api/business-trip/page", params);
export const submitBusinessTrip = (data: any) => post("/api/business-trip/submit", data);

// Outing
export const getOutingPage = (params: any) => get("/api/outing/page", params);
export const submitOuting = (data: any) => post("/api/outing/submit", data);

// Overtime
export const getOvertimePage = (params: any) => get("/api/overtime/page", params);
export const submitOvertime = (data: any) => post("/api/overtime/submit", data);

// Purchase
export const getPurchasePage = (params: any) => get("/api/purchase/page", params);
export const submitPurchase = (data: any) => post("/api/purchase/submit", data);

// Expense
export const getExpensePage = (params: any) => get("/api/expense/page", params);
export const submitExpense = (data: any) => post("/api/expense/submit", data);

// Loan
export const getLoanPage = (params: any) => get("/api/loan/page", params);
export const submitLoan = (data: any) => post("/api/loan/submit", data);
