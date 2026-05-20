import request from "@/utils/request";

export interface DocumentVO {
  id?: number;
  docName: string;
  fileType?: string;
  uploaderId?: number;
  uploaderName?: string;
  uploadTime?: string;
  fileSize?: number;
  filePath?: string;
}

export const getDocumentPage = (params: {
  pageNum: number;
  pageSize: number;
}) => {
  return request.get<any, any>("/api/document/page", { params });
};

export const uploadDocument = (file: File, uploaderId: number) => {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("uploaderId", String(uploaderId));
  return request.post("/api/document/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" }
  });
};

export const deleteDocument = (id: number) => {
  return request.delete(`/api/document/${id}`);
};

export const downloadDocument = (id: string | number) => {
  return request.get(`/api/document/download/${id}`, { responseType: "blob" });
};
