import { STATUS_MAP, STATUS_TAG_TYPE, ATTENDANCE_STATUS_MAP, ATTENDANCE_STATUS_TAG_TYPE } from "@/utils/constants";

/**
 * Format an ISO datetime string for display (e.g. "2025-01-01T12:30:00" -> "2025-01-01 12:30").
 */
export const formatTime = (time?: string | null) => {
  if (!time) return "-";
  return time.replace("T", " ").substring(0, 16);
};

/**
 * Format a date string for display (e.g. "2025-01-01T12:30:00" -> "2025-01-01").
 */
export const formatDate = (time?: string | null) => {
  if (!time) return "-";
  return time.replace("T", " ").substring(0, 10);
};

/**
 * Map an approval status code to its Chinese label.
 * 0=pending, 1=approved, 2=rejected, 3=withdrawn
 */
export const formatStatusText = (status?: number): string => {
  return STATUS_MAP[status ?? -1] || "未知";
};

/**
 * Map an approval status code to an Element Plus tag type.
 */
export const formatStatusTagType = (status?: number): string => {
  return STATUS_TAG_TYPE[status ?? -1] || "info";
};

/**
 * Map an attendance status code to its Chinese label.
 * 0=normal, 1=late, 2=early_leave, 3=absent, 4=rest, 5=leave, 6=business_trip
 */
export const formatAttendanceStatusText = (status?: number): string => {
  return ATTENDANCE_STATUS_MAP[status ?? -1] || "未知";
};

/**
 * Map an attendance status code to an Element Plus tag type.
 */
export const formatAttendanceStatusTagType = (status?: number): string => {
  return ATTENDANCE_STATUS_TAG_TYPE[status ?? -1] || "info";
};
