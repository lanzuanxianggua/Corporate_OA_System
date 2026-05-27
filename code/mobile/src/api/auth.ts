import { get, post } from "@/utils/request";

export const getCaptcha = () => get("/api/auth/captcha");

export const login = (data: { username: string; password: string; captchaUuid: string; captchaCode: string }) =>
  post("/login", data);

export const refreshToken = (refreshToken: string) =>
  post("/refresh-token", { refreshToken });
