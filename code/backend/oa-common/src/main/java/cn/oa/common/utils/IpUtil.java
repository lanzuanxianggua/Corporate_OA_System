package cn.oa.common.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP address utility for extracting client IP from HTTP requests.
 * Handles proxy headers (X-Forwarded-For, X-Real-IP) and falls back to remote address.
 */
public class IpUtil {

    private IpUtil() {
    }

    /**
     * Extract the client IP address from the request, accounting for proxies.
     *
     * @param request HTTP request
     * @return client IP address, never null
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
