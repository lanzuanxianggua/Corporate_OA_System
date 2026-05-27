import request from "@/utils/request";

export const getTodoPage = (params: any) =>
  request.get<any, any>("/api/todo/page", { params });

export const getTodoCount = () =>
  request.get<any, any>("/api/todo/count");

export const doneTodo = (id: number) =>
  request.post(`/api/todo/done/${id}`);

export const ignoreTodo = (id: number) =>
  request.post(`/api/todo/ignore/${id}`);
