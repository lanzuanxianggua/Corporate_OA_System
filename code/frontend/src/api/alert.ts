import request from "@/utils/request";

export const getAlertRulePage = (params: any) =>
  request.get<any, any>("/api/alert/rule/page", { params });

export const addAlertRule = (data: any) =>
  request.post("/api/alert/rule", data);

export const updateAlertRule = (data: any) =>
  request.put("/api/alert/rule", data);

export const deleteAlertRule = (id: number) =>
  request.delete(`/api/alert/rule/${id}`);

export const getAlertLogPage = (params: any) =>
  request.get<any, any>("/api/alert/log/page", { params });

export const handleAlert = (data: any) =>
  request.post("/api/alert/handle", data);
