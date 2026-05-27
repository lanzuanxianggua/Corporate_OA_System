import { get, post } from "@/utils/request";

export const getDefinitions = () => get("/api/workflow/definition/list");
export const getPendingTasks = (params: any) => get("/api/workflow/task/pending", params);
export const handleTask = (data: any) => post("/api/workflow/task/handle", data);
export const transferTask = (data: any) => post("/api/workflow/task/transfer", data);
export const returnTask = (data: any) => post("/api/workflow/task/return", data);
export const getApprovalHistory = (params: any) => get("/api/workflow/history", params);
export const getApprovalChain = (params: any) => get("/api/workflow/approval-chain", params);
export const withdrawApplication = (data: any) => post("/api/workflow/withdraw", data);
export const findPendingTask = (params: any) => get("/api/workflow/task/find", params);
