import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, Todo } from "@/types/api";

export const getTodoPage = (params: PageParams & Partial<Todo>) =>
  request.get<unknown, ApiResponse<PageResult<Todo>>>("/api/todo/page", { params });

export const getTodoCount = () =>
  request.get<unknown, ApiResponse<number>>("/api/todo/count");

export const doneTodo = (id: number) =>
  request.post<unknown, ApiResponse<void>>(`/api/todo/done/${id}`);

export const ignoreTodo = (id: number) =>
  request.post<unknown, ApiResponse<void>>(`/api/todo/ignore/${id}`);
