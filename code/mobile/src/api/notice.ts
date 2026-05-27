import { get, post } from "@/utils/request";

export const getNoticePage = (params: any) => get("/api/notice/page", params);
export const getNoticeById = (id: number) => get(`/api/notice/${id}`);
export const markNoticeAsRead = (id: number) => post(`/api/notice/${id}/read`);
