import { get, post, del } from "@/utils/request";

export const getDocumentPage = (params: any) => get("/api/document/page", params);
export const uploadDocument = (filePath: string) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync("token");
    uni.uploadFile({
      url: "/api/document/upload",
      filePath,
      name: "file",
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        const data = JSON.parse(res.data);
        if (data.code === 0) resolve(data);
        else reject(data);
      },
      fail: reject
    });
  });
};
export const deleteDocument = (id: number) => del(`/api/document/${id}`);
