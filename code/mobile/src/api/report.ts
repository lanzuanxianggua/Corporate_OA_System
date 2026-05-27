import { get } from "@/utils/request";

export const getPersonalAttendanceSummary = (params: any) => get("/api/report/personal/attendance-summary", params);
export const getPersonalLeaveSummary = (params: any) => get("/api/report/personal/leave-summary", params);
