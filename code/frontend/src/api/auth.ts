import request from "@/utils/request";

export interface LoginDTO {
  username: string;
  password: string;
  captchaUuid: string;
  captchaCode: string;
}

export interface LoginVO {
  accessToken: string;
  refreshToken: string;
  expires: string;
  username?: string;
  nickname?: string;
  avatar?: string;
  roles?: string[];
  permissions?: string[];
}

export const login = (data: LoginDTO) => {
  return request.post<any, any>("/login", data);
};

export const getCaptcha = () => {
  return request.get<any, any>("/api/auth/captcha");
};

export const refreshToken = (refreshToken: string) => {
  return request.post("/refresh-token", { refreshToken });
};

export const logout = () => {
  return request.post("/logout");
};
