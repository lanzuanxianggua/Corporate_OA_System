import request from "@/utils/request";
import type { ApiResponse, PageResult, Message } from "@/types/api";

export const getUnreadCount = () => {
  return request.get<unknown, ApiResponse<number>>("/api/message/unread-count");
};

export const getMessagePage = (params: {
  pageNum: number;
  pageSize: number;
}) => {
  return request.get<unknown, ApiResponse<PageResult<Message>>>("/api/message/page", { params });
};

export const sendMessage = (data: Partial<Message>) => {
  return request.post<unknown, ApiResponse<void>>("/api/message/send", data);
};

export const markAsRead = (id: string | number) => {
  return request.post<unknown, ApiResponse<void>>(`/api/message/${id}/read`);
};
