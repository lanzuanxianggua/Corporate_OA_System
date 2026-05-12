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

/** 获取公告列表 */
export const getNoticePage = (params?: object) => {
  return http.request<PageResult>("get", "/api/notice/page", { params });
};

/** 获取公告详情 */
export const getNoticeDetail = (id: number) => {
  return http.request<Result>("get", `/api/notice/${id}`);
};

/** 标记已读 */
export const markNoticeRead = (id: number) => {
  return http.request<Result>("post", `/api/notice/read/${id}`);
};

/** 管理员-新增公告 */
export const addNotice = (data?: object) => {
  return http.request<Result>("post", "/api/notice", { data });
};

/** 管理员-发布公告 */
export const publishNotice = (id: number) => {
  return http.request<Result>("put", `/api/notice/publish/${id}`);
};

/** 管理员-撤回公告 */
export const withdrawNotice = (id: number) => {
  return http.request<Result>("put", `/api/notice/withdraw/${id}`);
};
