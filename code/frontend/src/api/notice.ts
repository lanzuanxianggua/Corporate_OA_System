import request from "@/utils/request";

export interface NoticeVO {
  id?: number;
  title: string;
  content: string;
  noticeType?: number;
  publisherId?: number;
  publisher?: string;
  status?: number;
  createTime?: string;
  updateTime?: string;
  isRead?: boolean;
}

export const getNoticePage = (params: {
  pageNum: number;
  pageSize: number;
}) => {
  return request.get<any, any>("/api/notice/page", { params });
};

export const getNoticeById = (id: number) => {
  return request.get<any, any>(`/api/notice/${id}`);
};

export const addNotice = (data: Partial<NoticeVO>) => {
  return request.post("/api/notice", data);
};

export const updateNotice = (data: Partial<NoticeVO>) => {
  return request.put("/api/notice", data);
};

export const deleteNotice = (id: number) => {
  return request.delete(`/api/notice/${id}`);
};

export const markNoticeAsRead = (id: number) => {
  return request.post(`/api/notice/${id}/read`);
};
