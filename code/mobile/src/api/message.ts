import { get, post } from "@/utils/request";

export const getUnreadCount = () => get("/api/message/unread-count");
export const getMessagePage = (params: any) => get("/api/message/page", params);
export const sendMessage = (data: any) => post("/api/message/send", data);
export const markAsRead = (id: number) => post(`/api/message/${id}/read`);
