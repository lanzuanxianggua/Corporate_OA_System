import axios from "axios";
import { ElMessage, ElLoading } from "element-plus";
import router from "@/router";

const request = axios.create({
  baseURL: "",
  timeout: 15000
});

let isRefreshing = false;
let pendingRequests: Array<(token: string) => void> = [];

// Global loading state
let activeRequests = 0;
let loadingInstance: ReturnType<typeof ElLoading.service> | null = null;
let loadingTimer: ReturnType<typeof setTimeout> | null = null;

function showLoading() {
  activeRequests++;
  if (loadingTimer) return; // already scheduled
  loadingTimer = setTimeout(() => {
    if (activeRequests > 0 && !loadingInstance) {
      loadingInstance = ElLoading.service({ fullscreen: true, lock: true, text: "加载中..." });
    }
    loadingTimer = null;
  }, 500);
}

function hideLoading() {
  activeRequests = Math.max(0, activeRequests - 1);
  if (activeRequests === 0) {
    if (loadingTimer) {
      clearTimeout(loadingTimer);
      loadingTimer = null;
    }
    if (loadingInstance) {
      loadingInstance.close();
      loadingInstance = null;
    }
  }
}

function parseJwtExp(token: string): number | null {
  try {
    const payload = token.split(".")[1];
    const decoded = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
    return decoded.exp ?? null;
  } catch {
    return null;
  }
}

function isTokenExpiringSoon(token: string): boolean {
  const exp = parseJwtExp(token);
  if (!exp) return false;
  return Date.now() / 1000 > exp - 300; // 5 minutes before expiry
}

request.interceptors.request.use(
  async (config) => {
    showLoading();
    let token = localStorage.getItem("token");
    if (token && isTokenExpiringSoon(token)) {
      const refreshToken = localStorage.getItem("refreshToken");
      if (refreshToken && !isRefreshing) {
        isRefreshing = true;
        try {
          const res = await axios.post("/refresh-token", { refreshToken });
          if (res.data?.code === 0 && res.data?.data?.accessToken) {
            token = res.data.data.accessToken;
            localStorage.setItem("token", token!);
            if (res.data.data.refreshToken) {
              localStorage.setItem("refreshToken", res.data.data.refreshToken);
            }
            pendingRequests.forEach(cb => cb(token!));
            pendingRequests = [];
          }
        } catch {
          // refresh failed, let request continue with old token
        } finally {
          isRefreshing = false;
        }
      } else if (isRefreshing) {
        return new Promise((resolve) => {
          pendingRequests.push((newToken: string) => {
            config.headers.Authorization = `Bearer ${newToken}`;
            resolve(config);
          });
        });
      }
    }
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    hideLoading();
    return Promise.reject(error);
  }
);

request.interceptors.response.use(
  (response) => {
    hideLoading();
    if (response.config.responseType === "blob") {
      return { data: response.data, headers: response.headers };
    }
    const res = response.data;
    if (res.code === 0 || res.code === 200) {
      return res;
    }
    ElMessage.error(res.message || "请求失败");
    return Promise.reject(new Error(res.message || "请求失败"));
  },
  (error) => {
    hideLoading();
    if (error.response?.status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("userInfo");
      router.push("/login");
      ElMessage.error("登录已过期，请重新登录");
    } else {
      const data = error.response?.data;
      if (data instanceof Blob) {
        data.text().then((text: string) => {
          try {
            const json = JSON.parse(text);
            ElMessage.error(json.message || "请求失败");
          } catch {
            ElMessage.error("网络错误");
          }
        });
      } else {
        ElMessage.error(data?.message || "网络错误");
      }
    }
    return Promise.reject(error);
  }
);

export default request;
