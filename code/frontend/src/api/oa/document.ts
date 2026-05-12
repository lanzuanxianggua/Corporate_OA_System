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

/** 获取文档列表 */
export const getDocumentPage = (params?: object) => {
  return http.request<PageResult>("get", "/api/document/page", { params });
};

/** 下载文档 */
export const downloadDocument = (id: number) => {
  return http.request<Result>("get", `/api/document/download/${id}`);
};

/** 获取文档分类 */
export const getDocumentCategories = () => {
  return http.request<Result>("get", "/api/document/categories");
};

/** 管理员-上传文档 */
export const uploadDocument = (data?: object) => {
  return http.request<Result>("post", "/api/document", { data });
};

/** 管理员-删除文档 */
export const deleteDocument = (id: number) => {
  return http.request<Result>("delete", `/api/document/${id}`);
};
