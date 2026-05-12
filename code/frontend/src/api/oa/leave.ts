import { http } from "@/utils/http";

type Result = {
  code: number;
  message: string;
  data?: any;
};

type PageResult = {
  code: number;
  message: string;
  data?: {
    list: Array<any>;
    total?: number;
    pageSize?: number;
    currentPage?: number;
  };
};

/** 提交请假 */
export const submitLeave = (data?: object) => {
  return http.request<Result>("post", "/api/leave/apply", { data });
};

/** 我的请假记录 */
export const getMyLeaves = (params?: object) => {
  return http.request<PageResult>("get", "/api/leave/my", { params });
};

/** 撤回请假 */
export const revokeLeave = (id: number) => {
  return http.request<Result>("put", `/api/leave/revoke/${id}`);
};

/** 待审批列表 */
export const getPendingLeaves = (params?: object) => {
  return http.request<PageResult>("get", "/api/leave/pending", { params });
};

/** 审批请假 */
export const approveLeave = (id: number, data?: object) => {
  return http.request<Result>("put", `/api/leave/approve/${id}`, { data });
};
