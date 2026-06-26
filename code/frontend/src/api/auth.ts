import request from "@/utils/request";
import type { ApiResponse, LoginDTO, LoginVO } from "@/types/api";

export const login = (data: LoginDTO) => {
  return request.post<unknown, ApiResponse<LoginVO>>("/api/auth/login", data);
};

export const getCaptcha = () => {
  return request.get<unknown, ApiResponse<{ uuid: string; img: string }>>("/api/auth/captcha");
};

export const refreshToken = (refreshToken: string) => {
  return request.post<unknown, ApiResponse<LoginVO>>("/refresh-token", { refreshToken });
};

export const logout = () => {
  return request.post<unknown, ApiResponse<void>>("/logout");
};

export const changePassword = (oldPassword: string, newPassword: string) => {
  return request.post<unknown, ApiResponse<void>>("/api/auth/change-password", { oldPassword, newPassword });
};

export interface RegisterDTO {
  username: string;
  email: string;
  password: string;
}

export interface RegisterResultVO {
  pendingActivation: boolean;
  message: string;
}

export const register = (data: RegisterDTO) => {
  return request.post<unknown, ApiResponse<RegisterResultVO>>("/api/auth/register", data);
};
