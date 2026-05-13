import request from "../utils/request";

export interface LeaveApplyVO {
  id?: number;
  empId: number;
  empName?: string;
  deptName?: string;
  type: string;
  startTime: string;
  endTime: string;
  days: number;
  reason: string;
  status: number;
  approverId?: number;
  approverName?: string;
  remark?: string;
  createTime?: string;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageSize: number;
  currentPage: number;
}

export const getLeavePage = (params: {
  pageNum: number;
  pageSize: number;
  empId?: number;
  status?: number;
}) => {
  return request.get<any, any>("/leave/page", { params });
};

export const submitLeave = (data: Partial<LeaveApplyVO>) => {
  return request.post("/leave/submit", data);
};

export const approveLeave = (data: {
  id: number;
  status: number;
  remark?: string;
}) => {
  return request.post("/leave/approve", data);
};