import { get, post } from "@/utils/request";
import type { ApiResponse, PageResult, Notice } from "@/types/api";

export const getNoticePage = (params: any) => get<ApiResponse<PageResult<Notice>>>("/api/notice/page", params);
export const getNoticeById = (id: number) => get<ApiResponse<Notice>>(`/api/notice/${id}`);
export const markNoticeAsRead = (id: number) => post<ApiResponse<null>>(`/api/notice/${id}/read`);
