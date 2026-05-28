import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, DictType, DictData } from "@/types/api";

export const getDictTypePage = (params: PageParams & Partial<DictType>) =>
  request.get<unknown, ApiResponse<PageResult<DictType>>>("/api/dict/type/page", { params });

export const addDictType = (data: Partial<DictType>) =>
  request.post<unknown, ApiResponse<void>>("/api/dict/type", data);

export const updateDictType = (data: Partial<DictType>) =>
  request.put<unknown, ApiResponse<void>>("/api/dict/type", data);

export const deleteDictType = (id: number) =>
  request.delete<unknown, ApiResponse<void>>(`/api/dict/type/${id}`);

export const getDictDataPage = (params: PageParams & Partial<DictData> & { dictType?: string }) =>
  request.get<unknown, ApiResponse<PageResult<DictData>>>("/api/dict/data/page", { params });

export const getDictDataByType = (dictType: string) =>
  request.get<unknown, ApiResponse<DictData[]>>(`/api/dict/data/type/${dictType}`);

export const addDictData = (data: Partial<DictData>) =>
  request.post<unknown, ApiResponse<void>>("/api/dict/data", data);

export const updateDictData = (data: Partial<DictData>) =>
  request.put<unknown, ApiResponse<void>>("/api/dict/data", data);

export const deleteDictData = (id: number) =>
  request.delete<unknown, ApiResponse<void>>(`/api/dict/data/${id}`);
