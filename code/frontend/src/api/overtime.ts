import request from "@/utils/request";

export const submitOvertime = (data: any) =>
  request.post("/api/overtime/submit", data);

export const approveOvertime = (data: any) =>
  request.post("/api/overtime/approve", data);

export const getOvertimePage = (params: any) =>
  request.get<any, any>("/api/overtime/page", { params });
