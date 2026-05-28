import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, EmpArchive } from "@/types/api";

export const getArchive = (empId: number) =>
  request.get<unknown, ApiResponse<EmpArchive>>(`/api/emp-archive/${empId}`);

export const saveArchive = (data: Partial<EmpArchive>) =>
  request.post<unknown, ApiResponse<void>>("/api/emp-archive", data);

export const getArchivePage = (params: PageParams & Partial<EmpArchive> & Record<string, unknown>) =>
  request.get<unknown, ApiResponse<PageResult<EmpArchive>>>("/api/emp-archive/page", { params });
