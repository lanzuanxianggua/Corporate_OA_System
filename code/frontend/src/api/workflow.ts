import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, WorkflowTask, ProcessDefinition, CcRecord, Delegation, ApprovalRecord } from "@/types/api";

export const getDefinitions = (params?: Partial<ProcessDefinition> & { pageNum?: number; pageSize?: number }) =>
  request.get<unknown, ApiResponse<PageResult<ProcessDefinition>>>("/api/workflow/definition/list", { params });

export const createDefinition = (data: Partial<ProcessDefinition>) =>
  request.post<unknown, ApiResponse<void>>("/api/workflow/definition", data);

export const updateDefinition = (data: Partial<ProcessDefinition>) =>
  request.post<unknown, ApiResponse<void>>("/api/workflow/definition", data);

export const activateDefinition = (id: number) =>
  request.post<unknown, ApiResponse<void>>("/api/workflow/definition/activate", { id });

export const getPendingTasks = (params: { pageNum: number; pageSize: number }) =>
  request.get<unknown, ApiResponse<PageResult<WorkflowTask>>>("/api/workflow/task/pending", { params });

export const getHandledTasks = (params: { pageNum: number; pageSize: number }) =>
  request.get<unknown, ApiResponse<PageResult<WorkflowTask>>>("/api/workflow/task/handled", { params });

export const handleTask = (data: { taskId: number; status: number; remark?: string }) =>
  request.post<unknown, ApiResponse<void>>("/api/workflow/task/handle", data);

export const getApprovalHistory = (params: { businessType: string; businessId: number }) =>
  request.get<unknown, ApiResponse<WorkflowTask[]>>("/api/workflow/history", { params });

export const getApprovalChain = (params: { businessType: string; businessId: number }) =>
  request.get<unknown, ApiResponse<ApprovalRecord[]>>("/api/workflow/approval-chain", { params });

export const withdrawApplication = (data: { businessType: string; businessId: number }) =>
  request.post<unknown, ApiResponse<void>>("/api/workflow/withdraw", data);

export const findPendingTask = (params: { businessType: string; businessId: number }) =>
  request.get<unknown, ApiResponse<WorkflowTask>>("/api/workflow/task/find", { params });

export const transferTask = (data: { taskId: number; toAssigneeId: number; reason?: string }) =>
  request.post<unknown, ApiResponse<void>>("/api/workflow/task/transfer", data);

export const returnTask = (data: { taskId: number; returnTarget: string; remark?: string }) =>
  request.post<unknown, ApiResponse<void>>("/api/workflow/task/return", data);

export const urgeTask = (data: { businessType: string; businessId: number }) =>
  request.post<unknown, ApiResponse<void>>("/api/workflow/task/urge", data);

export const getMyCcRecords = (params: { pageNum: number; pageSize: number }) =>
  request.get<unknown, ApiResponse<PageResult<CcRecord>>>("/api/workflow/cc/my", { params });

export const readCcRecord = (id: number) =>
  request.post<unknown, ApiResponse<void>>(`/api/workflow/cc/read/${id}`);

export const setDelegation = (data: { delegateToId: number; startTime: string; endTime: string }) =>
  request.post<unknown, ApiResponse<void>>("/api/workflow/delegation/set", data);

export const getMyDelegations = () =>
  request.get<unknown, ApiResponse<Delegation[]>>("/api/workflow/delegation/my");

export const cancelDelegation = (id: number) =>
  request.post<unknown, ApiResponse<void>>(`/api/workflow/delegation/cancel/${id}`);

// V1010: graph-format validation + path preview
export const validateDefinitionApi = (data: { nodeConfig: string }) =>
  request.post<unknown, ApiResponse<any[]>>("/api/workflow/definition/validate", data);

export const previewDefinition = (params: { businessType: string; businessId: number }) =>
  request.get<unknown, ApiResponse<any[]>>("/api/workflow/definition/preview", { params });
