import { defineStore } from "pinia";
import { post, get } from "@/utils/request";

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
    userInfo: (uni.getStorageSync("userInfo") || {}) as UserInfo
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.userInfo.roles?.some((r) => r.toUpperCase() === "ADMIN") || false,
    displayName: (state) => state.userInfo.empName || "未登录"
  },

  actions: {
    async login(username: string, password: string, captchaUuid: string, captchaCode: string) {
      const res: any = await post("/login", { username, password, captchaUuid, captchaCode });
      this.token = res.data.accessToken;
      this.refreshToken = res.data.refreshToken;
      this.userInfo = {
        id: res.data.id,
        empName: res.data.nickname || res.data.username,
        empCode: res.data.username,
        roles: res.data.roles || []
      };
      uni.setStorageSync("token", this.token);
      uni.setStorageSync("refreshToken", this.refreshToken);
      uni.setStorageSync("userInfo", this.userInfo);
    },

    async logout() {
      try { await post("/logout"); } catch {}
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
        const res: any = await get("/mine");
        if (res.data) {
          this.userInfo = { ...this.userInfo, ...res.data };
          uni.setStorageSync("userInfo", this.userInfo);
        }
      } catch {}
    }
  }
});
