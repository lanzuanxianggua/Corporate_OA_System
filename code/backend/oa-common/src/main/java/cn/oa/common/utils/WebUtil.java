package cn.oa.common.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web层工具类 - 提取当前登录用户信息
 */
public final class WebUtil {

    private WebUtil() {
    }

    /**
     * 从请求属性中提取当前登录用户ID
     *
     * @param request HTTP请求
     * @return 用户ID，未登录返回null
     */
    public static Long getEmpId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object empIdObj = request.getAttribute("empId");
        if (empIdObj == null) {
            return null;
        }
        if (empIdObj instanceof Number) {
            return ((Number) empIdObj).longValue();
        }
        return Long.valueOf(empIdObj.toString());
    }

    /**
     * 从请求属性中提取当前登录用户名
     *
     * @param request HTTP请求
     * @return 用户名，未登录返回null
     */
    public static String getEmpName(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object empNameObj = request.getAttribute("empName");
        return empNameObj != null ? empNameObj.toString() : null;
    }
}
