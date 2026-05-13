import request from "@/utils/request";

export interface LeaveApplyVO {
  id?: number;
  empId?: number;
  empName?: string;
  leaveType?: number;
  startTime?: string;
  endTime?: string;
  reason?: string;
  status?: number;
  createTime?: string;
  remark?: string;
}

export interface PageResult<T> {
  list: T[];
  total: number;
}

export const getLeavePage = (params: {
  pageNum: number;
  pageSize: number;
  empId?: number;
  status?: number;
}) => {
  return request.get<any, any>("/api/leave/page", { params });
};

export const submitLeave = (data: Partial<LeaveApplyVO>) => {
  return request.post("/api/leave/submit", data);
};

export const approveLeave = (data: {
  id: number;
  status: number;
  remark?: string;
}) => {
  return request.post("/api/leave/approve", data);
};
