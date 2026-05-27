import { get, post } from "@/utils/request";

export const getTodoPage = (params: any) => get("/api/todo/page", params);
export const getTodoCount = () => get("/api/todo/count");
export const doneTodo = (id: number) => post(`/api/todo/done/${id}`);
