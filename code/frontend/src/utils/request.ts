import axios from "axios";
import { ElMessage } from "element-plus";
import router from "@/router";

const request = axios.create({
  baseURL: "",
  timeout: 15000
});

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code === 0 || res.code === 200) {
      return res;
    }
    ElMessage.error(res.message || "请求失败");
    return Promise.reject(new Error(res.message || "请求失败"));
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("userInfo");
      router.push("/login");
      ElMessage.error("登录已过期，请重新登录");
    } else {
      ElMessage.error(error.response?.data?.message || "网络错误");
    }
    return Promise.reject(error);
  }
);

export default request;
