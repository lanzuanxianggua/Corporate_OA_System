import { apiGet, apiPost } from "@/utils/http";
import request from "@/utils/request";
import type { ApiResponse } from "@/types/api";

// ── New API types (matching backend DTOs) ────────────────────────────────────

export interface LoginDTO {
  username: string;
  password: string;
}

export interface UserInfo {
  empId: number;
  /** Alias for empId — many existing views reference `id` */
  id?: number;
  username: string;
  realName: string;
  /** Alias for realName — many existing views reference `empName` */
  empName?: string;
  deptId: number;
  deptName: string;
  permissions: string[];
  roles: string[];
  dataScope: string;
}

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
  userInfo: UserInfo;
}

// ── New API methods (apiGet / apiPost unwrap ApiResponse automatically) ───────

export const authApi = {
  /** POST /api/auth/login */
  login(data: LoginDTO) {
    return apiPost<LoginResult>("/api/auth/login", data);
  },

  /** GET /api/auth/me */
  getCurrent() {
    return apiGet<UserInfo>("/api/auth/me");
  },

  /** POST /api/auth/refresh */
  refresh() {
    return apiPost<{ accessToken: string }>("/api/auth/refresh", {});
  },
};

// ── Legacy exports (kept for backward compat with existing views) ────────────

export const getCaptcha = () => {
  return request.get<unknown, ApiResponse<{ uuid: string; img: string }>>(
    "/api/auth/captcha"
  );
};

export const refreshToken = (refreshToken: string) => {
  return request.post<unknown, ApiResponse<{ accessToken: string; refreshToken: string }>>(
    "/refresh-token",
    { refreshToken }
  );
};

export const logout = () => {
  return request.post<unknown, ApiResponse<void>>("/logout");
};

export const changePassword = (oldPassword: string, newPassword: string) => {
  return request.post<unknown, ApiResponse<void>>("/api/auth/change-password", {
    oldPassword,
    newPassword,
  });
};
