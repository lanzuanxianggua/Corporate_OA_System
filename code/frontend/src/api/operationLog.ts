import request from "@/utils/request";

export const getOperationLogPage = (params: {
  pageNum: number;
  pageSize: number;
  module?: string;
  startTime?: string;
  endTime?: string;
}) => {
  return request.get<any, any>("/api/operation-log/page", { params });
};
