/**
 * Request utility for uni-app HTTP calls.
 *
 * BASE_URL logic:
 * - H5 dev: manifest.json devServer proxy forwards /api, /login, /refresh-token to backend.
 *   Do NOT set VITE_API_BASE_URL for H5 builds.
 * - Mini program: set VITE_API_BASE_URL in .env files to the full backend URL
 *   (e.g. http://localhost:8080 for dev, https://api.example.com for production).
 */
const BASE_URL: string = (import.meta as any).env?.VITE_API_BASE_URL || "";

interface RequestOptions {
  url: string;
  method?: "GET" | "POST" | "PUT" | "DELETE";
  data?: any;
  header?: Record<string, string>;
}

function getToken(): string {
  return uni.getStorageSync("token") || "";
}

function getRefreshToken(): string {
  return uni.getStorageSync("refreshToken") || "";
}

/** Clear auth state and redirect to login page */
function handleUnauthorized(): void {
  uni.removeStorageSync("token");
  uni.removeStorageSync("refreshToken");
  uni.removeStorageSync("userInfo");
  uni.reLaunch({ url: "/pages/login/index" });
}

/** Whether a token refresh is currently in flight */
let isRefreshing = false;
/** Queue of requests waiting for token refresh */
let pendingRequests: Array<() => void> = [];

function onTokenRefreshed(): void {
  pendingRequests.forEach((cb) => cb());
  pendingRequests = [];
}

/**
 * Attempt to refresh the access token using the stored refresh token.
 * Returns true if refresh succeeded.
 */
async function tryRefreshToken(): Promise<boolean> {
  const rt = getRefreshToken();
  if (!rt) return false;

  try {
    const res = await new Promise<any>((resolve, reject) => {
      uni.request({
        url: BASE_URL + "/refresh-token",
        method: "POST",
        data: { refreshToken: rt },
        header: { "Content-Type": "application/json" },
        success: (r) => resolve(r),
        fail: (e) => reject(e)
      });
    });

    const body = res.data as any;
    if (res.statusCode === 200 && body?.code === 0 && body?.data?.accessToken) {
      uni.setStorageSync("token", body.data.accessToken);
      if (body.data.refreshToken) {
        uni.setStorageSync("refreshToken", body.data.refreshToken);
      }
      return true;
    }
  } catch {}

  return false;
}

function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const token = getToken();
    const header: Record<string, string> = {
      "Content-Type": "application/json",
      ...(options.header || {})
    };
    if (token) {
      header["Authorization"] = `Bearer ${token}`;
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || "GET",
      data: options.data,
      header,
      success: async (res) => {
        // HTTP 401 — try refresh once
        if (res.statusCode === 401) {
          if (!isRefreshing) {
            isRefreshing = true;
            const ok = await tryRefreshToken();
            isRefreshing = false;
            if (ok) {
              onTokenRefreshed();
              // Retry original request with new token
              request<T>(options).then(resolve).catch(reject);
              return;
            }
            // Refresh failed — force login
            handleUnauthorized();
            reject(new Error("登录已过期"));
          } else {
            // Another request is already refreshing — queue this one
            pendingRequests.push(() => {
              request<T>(options).then(resolve).catch(reject);
            });
          }
          return;
        }

        const data = res.data as any;
        // Backend uses code 0 for success
        if (data?.code === 0) {
          resolve(data);
        } else if (data?.code === 401) {
          // Business-layer 401
          handleUnauthorized();
          reject(new Error(data.message || "未授权"));
        } else {
          uni.showToast({ title: data?.message || "请求失败", icon: "none" });
          reject(new Error(data?.message || "请求失败"));
        }
      },
      fail: (err) => {
        uni.showToast({ title: "网络异常", icon: "none" });
        reject(err);
      }
    });
  });
}

export function get<T = any>(url: string, data?: any): Promise<T> {
  return request<T>({ url, method: "GET", data });
}

export function post<T = any>(url: string, data?: any): Promise<T> {
  return request<T>({ url, method: "POST", data });
}

export function put<T = any>(url: string, data?: any): Promise<T> {
  return request<T>({ url, method: "PUT", data });
}

export function del<T = any>(url: string, data?: any): Promise<T> {
  return request<T>({ url, method: "DELETE", data });
}

export function upload(url: string, filePath: string, name: string = "file"): Promise<any> {
  return new Promise((resolve, reject) => {
    const token = getToken();
    uni.uploadFile({
      url: BASE_URL + url,
      filePath,
      name,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        const data = JSON.parse(res.data);
        if (data.code === 0) {
          resolve(data);
        } else if (data.code === 401) {
          handleUnauthorized();
          reject(new Error(data.message || "未授权"));
        } else {
          uni.showToast({ title: data.message || "上传失败", icon: "none" });
          reject(new Error(data.message));
        }
      },
      fail: reject
    });
  });
}
