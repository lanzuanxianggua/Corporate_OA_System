import { get, post } from "@/utils/request";
import type { ApiResponse, PageResult, WorkflowTask, WfProcessDefinition } from "@/types/api";

export const getDefinitions = () => get<ApiResponse<WfProcessDefinition[]>>("/api/workflow/definition/list");
export const getPendingTasks = (params: any) => get<ApiResponse<PageResult<WorkflowTask>>>("/api/workflow/task/pending", params);
export const getHandledTasks = (params: any) => get<ApiResponse<PageResult<WorkflowTask>>>("/api/workflow/task/handled", params);
export const handleTask = (data: any) => post<ApiResponse<null>>("/api/workflow/task/handle", data);
export const transferTask = (data: any) => post<ApiResponse<null>>("/api/workflow/task/transfer", data);
export const returnTask = (data: any) => post<ApiResponse<null>>("/api/workflow/task/return", data);
export const getApprovalHistory = (params: any) => get<ApiResponse<any>>("/api/workflow/history", params);
export const getApprovalChain = (params: any) => get<ApiResponse<any>>("/api/workflow/approval-chain", params);
export const withdrawApplication = (data: any) => post<ApiResponse<null>>("/api/workflow/withdraw", data);
export const findPendingTask = (params: any) => get<ApiResponse<WorkflowTask>>("/api/workflow/task/find", params);
