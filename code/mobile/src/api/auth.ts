import { get, post } from "@/utils/request";

export const getCaptcha = () => get("/api/auth/captcha");

export const login = (data: { username: string; password: string; captchaUuid: string; captchaCode: string }) =>
  post("/login", data);

export const refreshToken = (refreshToken: string) =>
  post("/refresh-token", { refreshToken });

export const changePassword = (data: { oldPassword: string; newPassword: string; confirmPassword: string }) =>
  post("/api/auth/change-password", data);
