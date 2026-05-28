import { del, get } from "@/utils/request";
import { upload } from "@/utils/request";

export const getDocumentPage = (params: any) => get("/api/document/page", params);

export const uploadDocument = (filePath: string) => upload("/api/document/upload", filePath);

export const deleteDocument = (id: number) => del(`/api/document/${id}`);
