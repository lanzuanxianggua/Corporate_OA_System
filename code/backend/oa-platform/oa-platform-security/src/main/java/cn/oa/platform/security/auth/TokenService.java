package cn.oa.platform.security.auth;

import cn.oa.platform.core.constant.Constants;
import cn.oa.platform.core.exception.AuthException;
import cn.oa.platform.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 服务
 * 负责 Token 的缓存管理和用户上下文管理
 *
 * @author oa-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    /**
     * ThreadLocal 存储用户上下文
     */
    private static final ThreadLocal<UserContext> USER_CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 创建并缓存 Token
     *
     * @param empId    员工ID
     * @param username 用户名
     * @param roles    角色列表
     * @return Token
     */
    public String createAndCacheToken(Long empId, String username, String roles) {
        String token = jwtUtils.generateToken(empId, username, roles);
        cacheToken(empId, token);
        return token;
    }

    /**
     * 缓存 Token 到 Redis
     *
     * @param empId 员工ID
     * @param token Token
     */
    public void cacheToken(Long empId, String token) {
        String key = Constants.REDIS_TOKEN_PREFIX + empId;
        long expireMillis = jwtUtils.getExpiration(token).getTime() - System.currentTimeMillis();
        long expireSeconds = expireMillis / 1000;
        if (expireSeconds > 0) {
            redisTemplate.opsForValue().set(key, token, expireSeconds, TimeUnit.SECONDS);
            log.debug("Token已缓存, empId={}, expireSeconds={}", empId, expireSeconds);
        }
    }

    /**
     * 从 Redis 获取缓存的 Token
     *
     * @param empId 员工ID
     * @return Token 或 null
     */
    public String getCachedToken(Long empId) {
        String key = Constants.REDIS_TOKEN_PREFIX + empId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 验证 Token（比对 Redis 缓存）
     *
     * @param token Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        if (!jwtUtils.validateToken(token)) {
            return false;
        }
        Long empId = jwtUtils.getEmpId(token);
        String cachedToken = getCachedToken(empId);
        return token.equals(cachedToken);
    }

    /**
     * 删除 Token（登出）
     *
     * @param empId 员工ID
     */
    public void removeToken(Long empId) {
        String key = Constants.REDIS_TOKEN_PREFIX + empId;
        redisTemplate.delete(key);
        clearUserContext();
        log.debug("Token已删除, empId={}", empId);
    }

    /**
     * 刷新 Token
     *
     * @param oldToken 原 Token
     * @return 新 Token
     */
    public String refreshToken(String oldToken) {
        if (!jwtUtils.canRefresh(oldToken)) {
            throw new AuthException(401, "Token无法刷新，请重新登录");
        }

        Long empId = jwtUtils.getEmpId(oldToken);
        String username = jwtUtils.getUsername(oldToken);
        String roles = jwtUtils.getRoles(oldToken);

        // 删除旧 Token
        removeToken(empId);

        // 创建新 Token
        return createAndCacheToken(empId, username, roles);
    }

    /**
     * 设置用户上下文到 ThreadLocal
     *
     * @param context 用户上下文
     */
    public void setUserContext(UserContext context) {
        USER_CONTEXT_HOLDER.set(context);
    }

    /**
     * 从 Token 解析并设置用户上下文
     *
     * @param token Token
     * @return 用户上下文
     */
    public UserContext parseAndSetUserContext(String token) {
        Long empId = jwtUtils.getEmpId(token);
        String username = jwtUtils.getUsername(token);
        String roles = jwtUtils.getRoles(token);

        UserContext context = UserContext.create(empId, username, roles);
        context.setToken(token);
        setUserContext(context);

        return context;
    }

    /**
     * 获取当前用户上下文
     *
     * @return 用户上下文
     */
    public UserContext getUserContext() {
        UserContext context = USER_CONTEXT_HOLDER.get();
        if (context == null) {
            throw new AuthException(401, "用户未登录");
        }
        return context;
    }

    /**
     * 获取当前用户上下文（可为空）
     *
     * @return 用户上下文或 null
     */
    public UserContext getUserContextOrNull() {
        return USER_CONTEXT_HOLDER.get();
    }

    /**
     * 获取当前员工ID
     *
     * @return 员工ID
     */
    public Long getCurrentEmpId() {
        return getUserContext().getEmpId();
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名
     */
    public String getCurrentUsername() {
        return getUserContext().getUsername();
    }

    /**
     * 判断当前用户是否为管理员
     *
     * @return 是否为管理员
     */
    public boolean isCurrentUserAdmin() {
        UserContext context = getUserContextOrNull();
        return context != null && context.isAdmin();
    }

    /**
     * 清除用户上下文
     */
    public void clearUserContext() {
        USER_CONTEXT_HOLDER.remove();
    }
}