package cn.oa.common.utils;

import cn.oa.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Web层工具类 - 提取当前登录用户信息、权限校验、通用数据转换
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

    /**
     * 强制数据归属校验: 非 ADMIN 用户只能操作自己的数据.
     * <p>委托 {@link AuthUtil#enforceOwnData(Long, Long)} 实现.</p>
     *
     * @param currentEmpId 当前登录员工 ID
     * @param targetEmpId  目标数据所属员工 ID（null 表示不限制）
     * @return 有效的目标 empId：
     *         <ul>
     *           <li>admin 传入 null → 返回 null（不限制）</li>
     *           <li>非 admin 传入 null → 返回 currentEmpId（强制查自己）</li>
     *           <li>admin 传入任意 empId → 返回该 empId</li>
     *           <li>非 admin 传入非本人 empId → 抛出 BusinessException</li>
     *           <li>非 admin 传入本人 empId → 返回该 empId</li>
     *         </ul>
     * @throws BusinessException 如果权限不足或未登录
     */
    public static Long enforceOwnDataAccess(Long currentEmpId, Long targetEmpId) {
        if (currentEmpId == null) {
            throw new BusinessException("未登录或令牌已过期");
        }
        // null target = 未指定过滤 → 非 admin 只能查自己
        if (targetEmpId == null) {
            if (!AuthUtil.isAdmin(currentEmpId)) {
                return currentEmpId;
            }
            return null;
        }
        // 非 null target → 检查数据归属
        AuthUtil.enforceOwnData(currentEmpId, targetEmpId);
        return targetEmpId;
    }

    /**
     * 批量查询员工并构建 empId → empName 映射.
     * <p>用于导出等场景中将 empId 转为员工姓名.</p>
     *
     * @param empIds       员工 ID 集合（null/empty 时返回空 Map）
     * @param batchQueryFn 批量查询函数，接收去重后的 ID 列表，返回实体列表
     * @param idExtractor  从实体中提取 ID 的函数（如 {@code SysEmployee::getId}）
     * @param nameExtractor 从实体中提取姓名的函数（如 {@code SysEmployee::getEmpName}）
     * @param <T>          员工实体类型
     * @return empId → empName 的 Map，不为 null
     */
    public static <T> Map<Long, String> buildEmployeeNameMap(
            Collection<Long> empIds,
            Function<List<Long>, List<T>> batchQueryFn,
            Function<T, Long> idExtractor,
            Function<T, String> nameExtractor) {
        if (empIds == null || empIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> distinctIds = empIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<T> employees = batchQueryFn.apply(distinctIds);
        if (employees == null || employees.isEmpty()) {
            return Collections.emptyMap();
        }
        return employees.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(idExtractor, nameExtractor, (a, b) -> a));
    }
}
