import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, SalaryStructure, SalaryRecord } from "@/types/api";

export const getStructurePage = (params: PageParams & Partial<SalaryStructure>) =>
  request.get<unknown, ApiResponse<PageResult<SalaryStructure>>>("/api/salary/structure/page", { params });

export const addStructure = (data: Partial<SalaryStructure>) =>
  request.post<unknown, ApiResponse<void>>("/api/salary/structure", data);

export const updateStructure = (data: Partial<SalaryStructure>) =>
  request.put<unknown, ApiResponse<void>>("/api/salary/structure", data);

export const getRecordPage = (params: PageParams & Partial<SalaryRecord>) =>
  request.get<unknown, ApiResponse<PageResult<SalaryRecord>>>("/api/salary/record/page", { params });

export const getMySalary = (params: { month?: string }) =>
  request.get<unknown, ApiResponse<SalaryRecord>>("/api/salary/my", { params });
