import request from "../utils/request";

export interface LoginDTO {
  username: string;
  password: string;
}

export interface LoginVO {
  token: string;
  refreshToken: string;
  expiresIn: number;
}

export const login = (data: LoginDTO) => {
  return request.post<any, any>("/login", data);
};

export const logout = () => {
  return request.post("/logout");
};

export const refreshToken = (refreshToken: string) => {
  return request.post("/refresh-token", { refreshToken });
};