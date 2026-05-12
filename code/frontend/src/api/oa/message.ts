import { http } from "@/utils/http";

type Result = {
  code: number;
  message: string;
  data?: any;
};

type PageResult = {
  code: number;
  message: string;
  data?: {
    list: Array<any>;
    total?: number;
    pageSize?: number;
    currentPage?: number;
  };
};

/** 获取消息列表 */
export const getMessagePage = (params?: object) => {
  return http.request<PageResult>("get", "/api/message/page", { params });
};

/** 获取未读消息数 */
export const getUnreadCount = () => {
  return http.request<Result>("get", "/api/message/unread-count");
};

/** 标记消息已读 */
export const markMessageRead = (id: number) => {
  return http.request<Result>("put", `/api/message/read/${id}`);
};

/** 批量标记已读 */
export const batchMarkRead = (data?: object) => {
  return http.request<Result>("put", "/api/message/batch-read", { data });
};

/** 管理员-发送系统消息 */
export const sendSystemMessage = (data?: object) => {
  return http.request<Result>("post", "/api/message/send-system", { data });
};
