import request from "@/utils/request";

export const getRooms = () =>
  request.get<any, any>("/api/meeting/room/list");

export const addRoom = (data: any) =>
  request.post("/api/meeting/room", data);

export const updateRoom = (data: any) =>
  request.put("/api/meeting/room", data);

export const deleteRoom = (id: number) =>
  request.delete(`/api/meeting/room/${id}`);

export const submitMeeting = (data: any) =>
  request.post("/api/meeting/submit", data);

export const getMeetingPage = (params: any) =>
  request.get<any, any>("/api/meeting/page", { params });

export const cancelMeeting = (id: number) =>
  request.post(`/api/meeting/cancel/${id}`);
