import { get, post } from "@/utils/request";
import type { ApiResponse, CaptchaResult, LoginResult, RefreshTokenResult, LoginParams } from "@/types/api";

export const getCaptcha = () => get<ApiResponse<CaptchaResult>>("/api/auth/captcha");

export const login = (data: LoginParams) =>
  post<ApiResponse<LoginResult>>("/login", data);

export const refreshToken = (refreshToken: string) =>
  post<ApiResponse<RefreshTokenResult>>("/refresh-token", { refreshToken });
