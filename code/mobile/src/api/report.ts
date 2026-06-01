import { get } from "@/utils/request";
import type { ApiResponse, AttendanceSummary, LeaveSummary } from "@/types/api";

export const getPersonalAttendanceSummary = (params: any) => get<ApiResponse<AttendanceSummary>>("/api/report/personal/attendance-summary", params);
export const getPersonalLeaveSummary = (params: any) => get<ApiResponse<LeaveSummary>>("/api/report/personal/leave-summary", params);
