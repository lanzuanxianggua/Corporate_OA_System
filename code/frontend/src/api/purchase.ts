import request from "@/utils/request";

export const getPurchasePage = (params: any) =>
  request.get<any, any>("/api/purchase/page", { params });

export const submitPurchase = (data: any) =>
  request.post("/api/purchase/submit", data);

export const approvePurchase = (data: any) =>
  request.post("/api/purchase/approve", data);
