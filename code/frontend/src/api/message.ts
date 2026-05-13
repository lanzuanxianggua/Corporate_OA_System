import request from "../utils/request";

export interface MessageVO {
  id: number;
  senderId: number;
  senderName: string;
  receiverId: number;
  receiverName?: string;
  title: string;
  content: string;
  priority: string;
  isRead: boolean;
  createTime: string;
}

export const getUnreadCount = () => {
  return request.get<any, any>("/message/unread-count");
};

export const sendMessage = (data: Partial<MessageVO>) => {
  return request.post("/message/send", data);
};

export const markAsRead = (id: number) => {
  return request.post(`/message/${id}/read`);
};