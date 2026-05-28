import request from "@/utils/request";
import type { ApiResponse, PageResult, OperationLog } from "@/types/api";

export const getOperationLogPage = (params: {
  pageNum: number;
  pageSize: number;
  module?: string;
  startTime?: string;
  endTime?: string;
}) => {
  return request.get<unknown, ApiResponse<PageResult<OperationLog>>>("/api/operation-log/page", { params });
};
