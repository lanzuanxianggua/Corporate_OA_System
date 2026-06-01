import { get } from "@/utils/request";
import type { ApiResponse, Employee } from "@/types/api";

export const getEmployeeById = (id: number) => get<ApiResponse<Employee>>(`/api/employee/${id}`);
