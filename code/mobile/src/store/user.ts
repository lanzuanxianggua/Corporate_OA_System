import { defineStore } from "pinia";
import { post, get } from "@/utils/request";
import { login as loginApi } from "@/api/auth";

interface UserInfo {
  id?: number;
  empName?: string;
  empCode?: string;
  phone?: string;
  email?: string;
  avatar?: string;
  roles?: string[];
  deptId?: number;
}

export const useUserStore = defineStore("user", {
  state: () => ({
    token: uni.getStorageSync("token") || "",
    refreshToken: uni.getStorageSync("refreshToken") || "",
    userInfo: (() => {
      try {
        return JSON.parse(uni.getStorageSync("userInfo") || "{}");
      } catch {
        return {};
      }
    })() as UserInfo
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.userInfo.roles?.some((r) => r.toUpperCase() === "ADMIN") || false,
    displayName: (state) => state.userInfo.empName || "未登录"
  },

  actions: {
    async login(username: string, password: string, captchaUuid: string, captchaCode: string) {
      const res: any = await loginApi({ username, password, captchaUuid, captchaCode });
      // Backend LoginVO fields: accessToken, refreshToken, username, nickname, avatar, roles, permissions
      const data = res.data;
      this.token = data.accessToken;
      this.refreshToken = data.refreshToken || "";
      this.userInfo = {
        empName: data.nickname || data.username,
        empCode: data.username,
        avatar: data.avatar || "",
        roles: data.roles || []
      };
      uni.setStorageSync("token", this.token);
      uni.setStorageSync("refreshToken", this.refreshToken);
      uni.setStorageSync("userInfo", JSON.stringify(this.userInfo));
    },

    async logout() {
      try {
        await post("/logout");
      } catch {}
      this.token = "";
      this.refreshToken = "";
      this.userInfo = {};
      uni.removeStorageSync("token");
      uni.removeStorageSync("refreshToken");
      uni.removeStorageSync("userInfo");
      uni.reLaunch({ url: "/pages/login/index" });
    },

    async fetchUserInfo() {
      try {
        // Use the employee endpoint to get current user info from token
        const res: any = await get("/api/employee/me");
        if (res.data) {
          this.userInfo = { ...this.userInfo, ...res.data };
          uni.setStorageSync("userInfo", JSON.stringify(this.userInfo));
        }
      } catch {
        // Endpoint may not exist — silently ignore
      }
    }
  }
});
