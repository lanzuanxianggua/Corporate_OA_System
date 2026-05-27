import { get, post } from "@/utils/request";

export const getLeavePage = (params: any) => get("/api/leave/page", params);
export const submitLeave = (data: any) => post("/api/leave/submit", data);
export const approveLeave = (data: any) => post("/api/leave/approve", data);
export const getMyBalances = () => get("/api/leave-balance/my");
