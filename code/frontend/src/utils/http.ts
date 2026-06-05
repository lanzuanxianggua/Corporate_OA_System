import request from "./request";
import type { ApiResponse } from "@/types/api";

/**
 * HTTP utility wrappers around the existing axios instance.
 *
 * Each function unwraps the ApiResponse envelope so callers receive
 * Promise<T> directly (the response interceptor in request.ts already
 * validates code === 0).
 */
export async function apiGet<T = any>(
  url: string,
  params?: Record<string, any>
): Promise<T> {
  const res = await request.get<unknown, ApiResponse<T>>(url, { params });
  return res.data as T;
}

export async function apiPost<T = any>(
  url: string,
  data?: any
): Promise<T> {
  const res = await request.post<unknown, ApiResponse<T>>(url, data);
  return res.data as T;
}

export async function apiPut<T = any>(
  url: string,
  data?: any
): Promise<T> {
  const res = await request.put<unknown, ApiResponse<T>>(url, data);
  return res.data as T;
}

export async function apiDelete<T = any>(
  url: string,
  params?: Record<string, any>
): Promise<T> {
  const res = await request.delete<unknown, ApiResponse<T>>(url, { params });
  return res.data as T;
}
