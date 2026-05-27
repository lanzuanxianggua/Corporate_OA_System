import request from "@/utils/request";

export const getContractPage = (params: any) =>
  request.get<any, any>("/api/contract/page", { params });

export const addContract = (data: any) =>
  request.post("/api/contract", data);

export const updateContract = (data: any) =>
  request.put("/api/contract", data);

export const deleteContract = (id: number) =>
  request.delete(`/api/contract/${id}`);

export const getExpiringContracts = (params: any) =>
  request.get<any, any>("/api/contract/expiring", { params });
