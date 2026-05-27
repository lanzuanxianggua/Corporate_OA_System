import request from "@/utils/request";

export const getPostList = () =>
  request.get<any, any>("/api/post/list");

export const getPostPage = (params: any) =>
  request.get<any, any>("/api/post/page", { params });

export const addPost = (data: any) =>
  request.post("/api/post", data);

export const updatePost = (data: any) =>
  request.put("/api/post", data);

export const deletePost = (id: number) =>
  request.delete(`/api/post/${id}`);
