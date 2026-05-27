import request from "@/utils/request";

export const getDefinitions = (params: any) =>
  request.get<any, any>("/api/workflow/definition/list", { params });

export const createDefinition = (data: any) =>
  request.post("/api/workflow/definition", data);

export const getPendingTasks = (params: any) =>
  request.get<any, any>("/api/workflow/task/pending", { params });

export const handleTask = (data: any) =>
  request.post("/api/workflow/task/handle", data);

export const getApprovalHistory = (params: { businessType: string; businessId: number }) =>
  request.get<any, any>("/api/workflow/history", { params });

export const getApprovalChain = (params: { businessType: string; businessId: number }) =>
  request.get<any, any>("/api/workflow/approval-chain", { params });

export const activateDefinition = (id: number) =>
  request.post("/api/workflow/definition/activate", { id });

export const updateDefinition = (data: any) =>
  request.post("/api/workflow/definition", data);

export const withdrawApplication = (data: { businessType: string; businessId: number }) =>
  request.post("/api/workflow/withdraw", data);

export const findPendingTask = (params: { businessType: string; businessId: number }) =>
  request.get<any, any>("/api/workflow/task/find", { params });

export const transferTask = (data: { taskId: number; toAssigneeId: number; reason?: string }) =>
  request.post("/api/workflow/task/transfer", data);

export const returnTask = (data: { taskId: number; returnTarget: string; remark?: string }) =>
  request.post("/api/workflow/task/return", data);

export const urgeTask = (data: { businessType: string; businessId: number }) =>
  request.post("/api/workflow/task/urge", data);

export const getMyCcRecords = (params: { pageNum: number; pageSize: number }) =>
  request.get<any, any>("/api/workflow/cc/my", { params });

export const readCcRecord = (id: number) =>
  request.post(`/api/workflow/cc/read/${id}`);

export const setDelegation = (data: { delegateToId: number; startTime: string; endTime: string }) =>
  request.post("/api/workflow/delegation/set", data);

export const getMyDelegations = () =>
  request.get<any, any>("/api/workflow/delegation/my");

export const cancelDelegation = (id: number) =>
  request.post(`/api/workflow/delegation/cancel/${id}`);
