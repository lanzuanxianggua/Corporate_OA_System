import request from "@/utils/request";

export const getStructurePage = (params: any) =>
  request.get<any, any>("/api/salary/structure/page", { params });

export const addStructure = (data: any) =>
  request.post("/api/salary/structure", data);

export const updateStructure = (data: any) =>
  request.put("/api/salary/structure", data);

export const getRecordPage = (params: any) =>
  request.get<any, any>("/api/salary/record/page", { params });

export const getMySalary = (params: any) =>
  request.get<any, any>("/api/salary/my", { params });
