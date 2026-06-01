package cn.oa.common.utils;

import cn.oa.common.constant.RoleConstants;
import cn.oa.common.exception.BusinessException;
import cn.oa.common.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 认证授权工具类 — 提供角色检查和数据权限校验的静态方法.
 */
@UtilityClass
public class AuthUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RedisService getRedisService() {
        return SpringContextHolder.getBean(RedisService.class);
    }

    /**
     * 判断指定员工是否为 ADMIN 角色.
     * 兼容 Redis 中 JSON 数组格式 (["ADMIN","USER"]) 和逗号分隔格式 ("ADMIN,USER").
     *
     * @param empId 员工 ID
     * @return true 如果包含 ADMIN 角色
     */
    public boolean isAdmin(Long empId) {
        if (empId == null) return false;
        Object roles = getRedisService().get("roles:" + empId);
        if (roles == null) return false;

        String rolesStr = roles.toString();
        if (rolesStr.isBlank()) return false;

        // 尝试解析为 JSON 数组
        if (rolesStr.startsWith("[")) {
            try {
                List<String> roleList = OBJECT_MAPPER.readValue(rolesStr, new TypeReference<List<String>>() {});
                return roleList.contains(RoleConstants.ADMIN);
            } catch (JsonProcessingException ignored) {
                // fall through to plain string check
            }
        }

        // 兼容逗号分隔格式
        return Arrays.asList(rolesStr.split(","))
                .stream()
                .map(String::trim)
                .anyMatch(role -> RoleConstants.ADMIN.equals(role));
    }

    /**
     * 强制数据归属校验: 非 ADMIN 用户只能操作自己的数据.
     *
     * @param currentEmpId 当前登录员工 ID
     * @param targetEmpId  目标数据所属员工 ID
     * @throws BusinessException 如果权限不足
     */
    public void enforceOwnData(Long currentEmpId, Long targetEmpId) {
        if (currentEmpId == null) {
            throw new BusinessException("未登录或令牌已过期");
        }
        if (targetEmpId == null) return; // null = 不限制
        if (!currentEmpId.equals(targetEmpId) && !isAdmin(currentEmpId)) {
            throw new BusinessException("无权访问他人的数据");
        }
    }
}
