import { get, post } from "@/utils/request";
import type { ApiResponse, PageResult, Message } from "@/types/api";

export const getUnreadCount = () => get<ApiResponse<number>>("/api/message/unread-count");
export const getMessagePage = (params: any) => get<ApiResponse<PageResult<Message>>>("/api/message/page", params);
export const sendMessage = (data: any) => post<ApiResponse<null>>("/api/message/send", data);
export const markAsRead = (id: number) => post<ApiResponse<null>>(`/api/message/${id}/read`);
