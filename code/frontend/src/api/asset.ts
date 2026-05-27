import request from "@/utils/request";

export const getAssetPage = (params: any) =>
  request.get<any, any>("/api/asset/page", { params });

export const addAsset = (data: any) =>
  request.post("/api/asset", data);

export const updateAsset = (data: any) =>
  request.put("/api/asset", data);

export const deleteAsset = (id: number) =>
  request.delete(`/api/asset/${id}`);

export const borrowAsset = (data: any) =>
  request.post("/api/asset/borrow", data);

export const returnAsset = (borrowId: number) =>
  request.post(`/api/asset/return/${borrowId}`);
