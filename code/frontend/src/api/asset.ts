import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, Asset, AssetBorrow } from "@/types/api";

export const getAssetPage = (params: PageParams & Partial<Asset>) =>
  request.get<unknown, ApiResponse<PageResult<Asset>>>("/api/asset/page", { params });

export const addAsset = (data: Partial<Asset>) =>
  request.post<unknown, ApiResponse<void>>("/api/asset", data);

export const updateAsset = (data: Partial<Asset>) =>
  request.put<unknown, ApiResponse<void>>("/api/asset", data);

export const deleteAsset = (id: number) =>
  request.delete<unknown, ApiResponse<void>>(`/api/asset/${id}`);

export const borrowAsset = (data: Partial<AssetBorrow>) =>
  request.post<unknown, ApiResponse<void>>("/api/asset/borrow", data);

export const returnAsset = (borrowId: number) =>
  request.post<unknown, ApiResponse<void>>(`/api/asset/return/${borrowId}`);

export const getBorrowPage = (params: PageParams & Partial<AssetBorrow>) =>
  request.get<unknown, ApiResponse<PageResult<AssetBorrow>>>("/api/asset/borrow/page", { params });
