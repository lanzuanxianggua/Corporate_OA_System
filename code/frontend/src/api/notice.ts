import request from "@/utils/request";
import type { ApiResponse, PageResult, Notice } from "@/types/api";

export const getNoticePage = (params: {
  pageNum: number;
  pageSize: number;
}) => {
  return request.get<unknown, ApiResponse<PageResult<Notice>>>("/api/notice/page", { params });
};

export const getNoticeById = (id: number) => {
  return request.get<unknown, ApiResponse<Notice>>(`/api/notice/${id}`);
};

export const addNotice = (data: Partial<Notice>) => {
  return request.post<unknown, ApiResponse<void>>("/api/notice", data);
};

export const updateNotice = (data: Partial<Notice>) => {
  return request.put<unknown, ApiResponse<void>>("/api/notice", data);
};

export const deleteNotice = (id: number) => {
  return request.delete<unknown, ApiResponse<void>>(`/api/notice/${id}`);
};

export const markNoticeAsRead = (id: number) => {
  return request.post<unknown, ApiResponse<void>>(`/api/notice/${id}/read`);
};
