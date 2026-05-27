import request from "@/utils/request";

export const getConfigPage = (params: any) =>
  request.get<any, any>("/api/config/page", { params });

export const getConfigByKey = (configKey: string) =>
  request.get<any, any>(`/api/config/key/${configKey}`);

export const addConfig = (data: any) =>
  request.post("/api/config", data);

export const updateConfig = (data: any) =>
  request.put("/api/config", data);

export const deleteConfig = (id: number) =>
  request.delete(`/api/config/${id}`);
