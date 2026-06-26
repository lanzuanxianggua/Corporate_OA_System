import { defineStore } from "pinia";
import { ref } from "vue";
import { login as loginApi, logout as logoutApi } from "@/api/auth";
import router from "@/router";
import type { LoginVO } from "@/types/api";

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

function parseJwtPayload(token: string) {
  try {
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(decodeURIComponent(atob(base64).split("").map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2)).join("")));
  } catch {
    return null;
  }
}

export const useUserStore = defineStore("user", () => {
  const token = ref(localStorage.getItem("token") || "");
  const userInfo = ref<UserStoreInfo | null>(null);

  // Initialize userInfo from localStorage with error handling
  try {
    const raw = localStorage.getItem("userInfo");
    if (raw) {
      userInfo.value = JSON.parse(raw);
    }
  } catch {
    localStorage.removeItem("userInfo");
  }

  const hasRole = (role: string) => {
    if (!userInfo.value?.roles) return false;
    const required = role.toUpperCase();
    return userInfo.value.roles.some((r: string) => r.toUpperCase() === required);
  };

  const hasAnyRole = (roles: string[]) => {
    if (!roles?.length) return true;
    if (!userInfo.value?.roles) return false;
    if (hasRole("ADMIN")) return true;
    return roles.some((role) => hasRole(role));
  };

  const isAdmin = () => hasRole("ADMIN");

  const hasPermission = (permission: string) => {
    if (isAdmin()) return true;
    const permissions = userInfo.value?.permissions || [];
    return permissions.includes("*:*:*") || permissions.includes(permission);
  };

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

  return { token, userInfo, hasRole, hasAnyRole, hasPermission, isAdmin, loginAction, logoutAction };
});
