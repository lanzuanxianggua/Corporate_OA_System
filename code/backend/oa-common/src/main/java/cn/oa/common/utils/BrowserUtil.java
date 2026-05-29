package cn.oa.common.utils;

/**
 * Browser and OS detection utility based on User-Agent header parsing.
 */
public class BrowserUtil {

    private BrowserUtil() {
    }

    /**
     * Detect browser name from a User-Agent string.
     *
     * @param userAgent the User-Agent header value
     * @return browser name or "unknown"
     */
    public static String getBrowser(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "unknown";
        }
        if (userAgent.contains("Edg")) return "Edge";
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari")) return "Safari";
        return "Other";
    }

    /**
     * Detect operating system from a User-Agent string.
     *
     * @param userAgent the User-Agent header value
     * @return OS name or "unknown"
     */
    public static String getOs(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "unknown";
        }
        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Mac")) return "Mac";
        if (userAgent.contains("Linux")) return "Linux";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) return "iOS";
        return "Other";
    }
}
