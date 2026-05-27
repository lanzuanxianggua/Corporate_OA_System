import request from "@/utils/request";

export const getArchive = (empId: number) =>
  request.get<any, any>(`/api/emp-archive/${empId}`);

export const saveArchive = (data: any) =>
  request.post("/api/emp-archive", data);

export const getArchivePage = (params: any) =>
  request.get<any, any>("/api/emp-archive/page", { params });
