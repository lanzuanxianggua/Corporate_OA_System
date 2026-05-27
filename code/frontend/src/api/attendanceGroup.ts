import request from "@/utils/request";

export const getAttendanceGroupPage = (params: any) =>
  request.get<any, any>("/api/attendance-group/page", { params });

export const addAttendanceGroup = (data: any) =>
  request.post("/api/attendance-group", data);

export const updateAttendanceGroup = (data: any) =>
  request.put("/api/attendance-group", data);

export const deleteAttendanceGroup = (id: number) =>
  request.delete(`/api/attendance-group/${id}`);

export const assignEmployees = (data: { groupId: number; empIds: number[] }) =>
  request.post(`/api/attendance-group/${data.groupId}/employees`, { empIds: data.empIds });

export const removeEmployees = (data: { groupId: number; empIds: number[] }) =>
  request.delete(`/api/attendance-group/${data.groupId}/employees`, { data: { empIds: data.empIds } });
