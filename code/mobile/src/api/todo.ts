import { get, post } from "@/utils/request";
import type { ApiResponse, PageResult, Todo } from "@/types/api";

export const getTodoPage = (params: any) => get<ApiResponse<PageResult<Todo>>>("/api/todo/page", params);
export const getTodoCount = () => get<ApiResponse<number>>("/api/todo/count");
export const doneTodo = (id: number) => post<ApiResponse<null>>(`/api/todo/done/${id}`);
