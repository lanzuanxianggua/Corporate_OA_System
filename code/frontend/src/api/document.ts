import request from "@/utils/request";
import type { ApiResponse, PageResult, Document } from "@/types/api";

export const getDocumentPage = (params: {
  pageNum: number;
  pageSize: number;
  keyword?: string;
}) => {
  return request.get<unknown, ApiResponse<PageResult<Document>>>("/api/document/page", { params });
};

export const uploadDocument = (file: File, uploaderId: number) => {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("uploaderId", String(uploaderId));
  return request.post<unknown, ApiResponse<Document>>("/api/document/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" }
  });
};

export const deleteDocument = (id: number) => {
  return request.delete<unknown, ApiResponse<void>>(`/api/document/${id}`);
};

export const downloadDocument = (id: string | number) => {
  return request.get(`/api/document/download/${id}`, {
    responseType: "blob",
    timeout: 60000
  });
};
