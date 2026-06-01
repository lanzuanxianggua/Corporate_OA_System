import { defineStore } from "pinia";
import { ref } from "vue";
import { login as loginApi, logout as logoutApi } from "@/api/auth";
import router from "@/router";
import type { LoginVO } from "@/types/api";
import { parseJwtPayload } from "@/utils/jwt";

interface UserStoreInfo {
  id?: number;
  username?: string;
  nickname?: string;
  empName?: string;
  empId?: number;
  phone?: string;
  email?: string;
  status?: number;
  avatar?: string;
  createTime?: string;
  accessToken?: string;
  refreshToken?: string;
  roles?: string[];
  permissions?: string[];
}

export const useUserStore = defineStore("user", () => {
  const token = ref(localStorage.getItem("token") || "");
  const userInfo = ref<UserStoreInfo | null>(
    localStorage.getItem("userInfo")
      ? JSON.parse(localStorage.getItem("userInfo")!)
      : null
  );

  const hasRole = (role: string) => {
    if (!userInfo.value?.roles) return false;
    return userInfo.value.roles.some(
      (r: string) => r === role || r === role.toUpperCase()
    );
  };

  const hasAnyRole = (roles: string[]) => {
    if (!roles?.length) return true;
    if (!userInfo.value?.roles) return false;
    return roles.some((role) => hasRole(role));
  };

  const isAdmin = () => hasRole("ADMIN");

  const loginAction = async (username: string, password: string, captchaUuid: string, captchaCode: string) => {
    const res = await loginApi({ username, password, captchaUuid, captchaCode });
    if (res.data) {
      const data = res.data as LoginVO;
      const claims = parseJwtPayload(data.accessToken);
      token.value = data.accessToken;
      userInfo.value = {
        ...data,
        empName: data.nickname,
        empId: claims?.empId
      } as UserStoreInfo;
      localStorage.setItem("token", data.accessToken);
      if (data.refreshToken) {
        localStorage.setItem("refreshToken", data.refreshToken);
      }
      localStorage.setItem("userInfo", JSON.stringify(userInfo.value));
    }
    return res;
  };

  const logoutAction = async () => {
    try {
      await logoutApi();
    } catch {
      // ignore errors on logout
    }
    token.value = "";
    userInfo.value = null;
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("userInfo");
    router.push("/login");
  };

  return { token, userInfo, hasRole, hasAnyRole, isAdmin, loginAction, logoutAction };
});
