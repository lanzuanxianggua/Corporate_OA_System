import request from "@/utils/request";

export const getDictTypePage = (params: any) =>
  request.get<any, any>("/api/dict/type/page", { params });

export const addDictType = (data: any) =>
  request.post("/api/dict/type", data);

export const updateDictType = (data: any) =>
  request.put("/api/dict/type", data);

export const deleteDictType = (id: number) =>
  request.delete(`/api/dict/type/${id}`);

export const getDictDataPage = (params: any) =>
  request.get<any, any>("/api/dict/data/page", { params });

export const getDictDataByType = (dictType: string) =>
  request.get<any, any>(`/api/dict/data/type/${dictType}`);

export const addDictData = (data: any) =>
  request.post("/api/dict/data", data);

export const updateDictData = (data: any) =>
  request.put("/api/dict/data", data);

export const deleteDictData = (id: number) =>
  request.delete(`/api/dict/data/${id}`);
