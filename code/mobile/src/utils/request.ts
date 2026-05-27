const BASE_URL = "";

interface RequestOptions {
  url: string;
  method?: "GET" | "POST" | "PUT" | "DELETE";
  data?: any;
  header?: Record<string, string>;
}

function getToken(): string {
  return uni.getStorageSync("token") || "";
}

function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
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
      success: (res) => {
        if (res.statusCode === 401) {
          uni.removeStorageSync("token");
          uni.removeStorageSync("refreshToken");
          uni.removeStorageSync("userInfo");
          uni.reLaunch({ url: "/pages/login/index" });
          reject(new Error("登录已过期"));
          return;
        }
        const data = res.data as any;
        if (data.code === 0 || data.code === 200) {
          resolve(data);
        } else if (data.code === 401) {
          uni.removeStorageSync("token");
          uni.removeStorageSync("refreshToken");
          uni.removeStorageSync("userInfo");
          uni.reLaunch({ url: "/pages/login/index" });
          reject(new Error(data.message || "未授权"));
        } else {
          uni.showToast({ title: data.message || "请求失败", icon: "none" });
          reject(new Error(data.message || "请求失败"));
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
        if (data.code === 0 || data.code === 200) {
          resolve(data);
        } else {
          uni.showToast({ title: data.message || "上传失败", icon: "none" });
          reject(new Error(data.message));
        }
      },
      fail: reject
    });
  });
}
