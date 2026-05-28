import request from "@/utils/request";
import type { ApiResponse, LoginDTO, LoginVO, CaptchaVO } from "@/types/api";

export const login = (data: LoginDTO) => {
  return request.post<unknown, ApiResponse<LoginVO>>("/login", data);
};

export const getCaptcha = () => {
  return request.get<unknown, ApiResponse<CaptchaVO>>("/api/auth/captcha");
};

export const refreshToken = (refreshToken: string) => {
  return request.post<unknown, ApiResponse<LoginVO>>("/refresh-token", { refreshToken });
};

export const logout = () => {
  return request.post<unknown, ApiResponse<void>>("/logout");
};
