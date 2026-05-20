import request from "@/utils/request";

export const getBusinessTripPage = (params: any) =>
  request.get<any, any>("/api/business-trip/page", { params });

export const submitBusinessTrip = (data: any) =>
  request.post("/api/business-trip/submit", data);

export const approveBusinessTrip = (data: any) =>
  request.post("/api/business-trip/approve", data);
