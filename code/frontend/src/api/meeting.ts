import request from "@/utils/request";
import type { ApiResponse, PageResult, PageParams, MeetingRoom, Meeting } from "@/types/api";

export const getRooms = () =>
  request.get<unknown, ApiResponse<MeetingRoom[]>>("/api/meeting/room/list");

export const addRoom = (data: Partial<MeetingRoom>) =>
  request.post<unknown, ApiResponse<void>>("/api/meeting/room", data);

export const updateRoom = (data: Partial<MeetingRoom>) =>
  request.put<unknown, ApiResponse<void>>("/api/meeting/room", data);

export const deleteRoom = (id: number) =>
  request.delete<unknown, ApiResponse<void>>(`/api/meeting/room/${id}`);

export const submitMeeting = (data: Partial<Meeting>) =>
  request.post<unknown, ApiResponse<void>>("/api/meeting/submit", data);

export const getMeetingPage = (params: PageParams & Partial<Meeting>) =>
  request.get<unknown, ApiResponse<PageResult<Meeting>>>("/api/meeting/page", { params });

export const cancelMeeting = (id: number) =>
  request.post<unknown, ApiResponse<void>>(`/api/meeting/cancel/${id}`);
