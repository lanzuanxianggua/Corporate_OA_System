import { get } from "@/utils/request";

export const getEmployeeById = (id: number) => get(`/api/employee/${id}`);
