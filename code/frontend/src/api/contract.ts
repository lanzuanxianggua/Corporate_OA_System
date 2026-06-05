import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, Contract } from "@/types/api";

export const getContractPage = (params: PageParams & Partial<Contract>) =>
  request.get<unknown, ApiResponse<PageResult<Contract>>>("/api/contract/page", { params });

export const addContract = (data: Partial<Contract>) =>
  request.post<unknown, ApiResponse<void>>("/api/contract", data);

export const updateContract = (data: Partial<Contract>) =>
  request.put<unknown, ApiResponse<void>>("/api/contract", data);

export const deleteContract = (id: number) =>
  request.delete<unknown, ApiResponse<void>>(`/api/contract/${id}`);

export const getExpiringContracts = (params: { days?: number } & Record<string, unknown>) =>
  request.get<unknown, ApiResponse<Contract[]>>("/api/contract/expiring", { params });
