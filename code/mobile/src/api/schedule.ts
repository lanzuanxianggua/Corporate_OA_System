import { get, post, put, del } from "@/utils/request";

export const getSchedulePage = (params: any) => get("/api/schedule/page", params);
export const addSchedule = (data: any) => post("/api/schedule", data);
export const updateSchedule = (data: any) => put("/api/schedule", data);
export const deleteSchedule = (id: number) => del(`/api/schedule/${id}`);
