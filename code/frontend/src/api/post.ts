import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, Post } from "@/types/api";

export const getPostList = () =>
  request.get<unknown, ApiResponse<Post[]>>("/api/post/list");

export const getPostPage = (params: PageParams & Partial<Post>) =>
  request.get<unknown, ApiResponse<PageResult<Post>>>("/api/post/page", { params });

export const addPost = (data: Partial<Post>) =>
  request.post<unknown, ApiResponse<void>>("/api/post", data);

export const updatePost = (data: Partial<Post>) =>
  request.put<unknown, ApiResponse<void>>("/api/post", data);

export const deletePost = (id: number) =>
  request.delete<unknown, ApiResponse<void>>(`/api/post/${id}`);
