import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, Config } from "@/types/api";

export const getConfigPage = (params: PageParams & Partial<Config> & Record<string, unknown>) =>
  request.get<unknown, ApiResponse<PageResult<Config>>>("/api/config/page", { params });

export const getConfigByKey = (configKey: string) =>
  request.get<unknown, ApiResponse<Config>>(`/api/config/key/${configKey}`);

export const addConfig = (data: Partial<Config>) =>
  request.post<unknown, ApiResponse<void>>("/api/config", data);

export const updateConfig = (data: Partial<Config>) =>
  request.put<unknown, ApiResponse<void>>("/api/config", data);

export const deleteConfig = (id: number) =>
  request.delete<unknown, ApiResponse<void>>(`/api/config/${id}`);
