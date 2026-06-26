import { get, post } from "@/utils/request";

export const getLeavePage = (params: any) => get("/api/leave/page", params);
export const submitLeave = (data: any) => post("/api/leave/submit", data);

export const getBusinessTripPage = (params: any) => get("/api/business-trip/page", params);
export const submitBusinessTrip = (data: any) => post("/api/business-trip/submit", data);

export const getOutingPage = (params: any) => get("/api/outing/page", params);
export const submitOuting = (data: any) => post("/api/outing/submit", data);

export const getOvertimePage = (params: any) => get("/api/overtime/page", params);
export const submitOvertime = (data: any) => post("/api/overtime/submit", data);

export const getPurchasePage = (params: any) => get("/api/purchase/page", params);
export const submitPurchase = (data: any) => post("/api/purchase/submit", data);

export const getExpensePage = (params: any) => get("/api/expense/page", params);
export const submitExpense = (data: any) => post("/api/expense/submit", data);

export const getLoanPage = (params: any) => get("/api/loan/page", params);
export const submitLoan = (data: any) => post("/api/loan/submit", data);
