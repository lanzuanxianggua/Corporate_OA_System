import { del, get } from "@/utils/request";
import { upload } from "@/utils/request";
import type { ApiResponse, PageResult, Document } from "@/types/api";

export const getDocumentPage = (params: any) => get<ApiResponse<PageResult<Document>>>("/api/document/page", params);

export const uploadDocument = (filePath: string) => upload<ApiResponse<Document>>("/api/document/upload", filePath);

export const deleteDocument = (id: number) => del<ApiResponse<null>>(`/api/document/${id}`);
