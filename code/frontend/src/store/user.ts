import { defineStore } from "pinia";
import { ref } from "vue";
import { login as loginApi, logout as logoutApi, getCaptcha } from "@/api/auth";
import router from "@/router";

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
  const userInfo = ref<any>(
    localStorage.getItem("userInfo")
      ? JSON.parse(localStorage.getItem("userInfo")!)
      : null
  );

  const isAdmin = () => {
    if (!userInfo.value?.roles) return false;
    return userInfo.value.roles.some(
      (r: string) => r === "ADMIN" || r === "admin"
    );
  };

  const loginAction = async (username: string, password: string, captchaUuid: string, captchaCode: string) => {
    const res: any = await loginApi({ username, password, captchaUuid, captchaCode });
    if (res.data) {
      const data = res.data;
      const claims = parseJwtPayload(data.accessToken);
      token.value = data.accessToken;
      userInfo.value = {
        ...data,
        empName: data.nickname,
        empId: claims?.empId
      };
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
      // ignore
    }
    token.value = "";
    userInfo.value = null;
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("userInfo");
    router.push("/login");
  };

  return { token, userInfo, isAdmin, loginAction, logoutAction };
});
