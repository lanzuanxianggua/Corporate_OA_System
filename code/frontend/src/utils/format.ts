/**
 * Format an ISO datetime string for display (e.g. "2025-01-01T12:30:00" -> "2025-01-01 12:30").
 */
export const formatTime = (time?: string | null) => {
  if (!time) return "-";
  return time.replace("T", " ").substring(0, 16);
};

/**
 * Map a status code to its Chinese label for OA approval workflows.
 * 0 = pending, 1 = approved, 2 = rejected.
 */
export const formatStatusText = (status?: number): string => {
  const map: Record<number, string> = {
    0: "待审批",
    1: "已通过",
    2: "已拒绝"
  };
  return map[status ?? -1] || "未知";
};

/**
 * Map a status code to an Element Plus tag type for OA approval workflows.
 */
export const formatStatusTagType = (status?: number): string => {
  const map: Record<number, string> = {
    0: "warning",
    1: "success",
    2: "danger"
  };
  return map[status ?? -1] || "info";
};
