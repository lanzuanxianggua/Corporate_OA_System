import request from "@/utils/request";

export interface MessageVO {
  id?: string;
  senderId?: string;
  senderName?: string;
  receiverId: number;
  receiverName?: string;
  msgType?: number;
  title: string;
  content: string;
  isRead?: number;
  createTime?: string;
}

export const getUnreadCount = () => {
  return request.get<any, any>("/api/message/unread-count");
};

export const getMessagePage = (params: {
  pageNum: number;
  pageSize: number;
}) => {
  return request.get<any, any>("/api/message/page", { params });
};

export const sendMessage = (data: Partial<MessageVO>) => {
  return request.post("/api/message/send", data);
};

export const markAsRead = (id: string | number) => {
  return request.post(`/api/message/${id}/read`);
};
