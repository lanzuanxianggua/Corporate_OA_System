import request from "@/utils/request";

export const getOutingPage = (params: any) =>
  request.get<any, any>("/api/outing/page", { params });

export const submitOuting = (data: any) =>
  request.post("/api/outing/submit", data);

export const approveOuting = (data: any) =>
  request.post("/api/outing/approve", data);
