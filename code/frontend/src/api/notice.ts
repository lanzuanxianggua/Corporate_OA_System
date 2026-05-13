import request from "../utils/request";

export interface NoticeVO {
  id?: number;
  title: string;
  content: string;
  publisherId?: number;
  publisherName?: string;
  publishTime?: string;
  urgent?: boolean;
  isRead?: boolean;
}

export const getNoticePage = (params: { pageNum: number; pageSize: number }) => {
  return request.get<any, any>("/notice/page", { params });
};

export const getNoticeById = (id: number) => {
  return request.get<any, any>(`/notice/${id}`);
};

export const addNotice = (data: Partial<NoticeVO>) => {
  return request.post("/notice", data);
};

export const updateNotice = (data: Partial<NoticeVO>) => {
  return request.put("/notice", data);
};

export const deleteNotice = (id: number) => {
  return request.delete(`/notice/${id}`);
};

export const markNoticeAsRead = (id: number) => {
  return request.post(`/notice/${id}/read`);
};