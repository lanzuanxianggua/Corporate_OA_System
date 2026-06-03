package cn.oa.platform.security.jwt;

import cn.oa.platform.core.exception.AuthException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 负责 JWT 的生成、解析和验证
 *
 * @author oa-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtConfig jwtConfig;

    /**
     * 签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token
     *
     * @param empId    员工ID
     * @param username 用户名
     * @param roles    角色列表
     * @return JWT Token
     */
    public String generateToken(Long empId, String username, String roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("empId", empId);
        claims.put("username", username);
        claims.put("roles", roles);
        return createToken(claims, username);
    }

    /**
     * 生成 JWT Token（自定义Claims）
     *
     * @param claims 自定义声明
     * @param subject 主题（通常是用户名）
     * @return JWT Token
     */
    public String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getExpireMillis());

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(jwtConfig.getIssuer())
                .audience().add(jwtConfig.getAudience()).and()
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 JWT Token
     *
     * @param token JWT Token
     * @return Claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token已过期: {}", e.getMessage());
            throw new AuthException(401, "Token已过期，请重新登录");
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的JWT Token: {}", e.getMessage());
            throw new AuthException(401, "不支持的Token格式");
        } catch (MalformedJwtException e) {
            log.warn("JWT Token格式错误: {}", e.getMessage());
            throw new AuthException(401, "Token格式错误");
        } catch (IllegalArgumentException e) {
            log.warn("JWT Token为空: {}", e.getMessage());
            throw new AuthException(401, "Token不能为空");
        } catch (Exception e) {
            log.error("JWT Token解析失败: {}", e.getMessage(), e);
            throw new AuthException(401, "Token解析失败");
        }
    }

    /**
     * 验证 JWT Token 是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !isTokenExpired(claims);
        } catch (AuthException e) {
            return false;
        }
    }

    /**
     * 从 Token 中获取员工ID
     *
     * @param token JWT Token
     * @return 员工ID
     */
    public Long getEmpId(String token) {
        Claims claims = parseToken(token);
        Object empId = claims.get("empId");
        if (empId instanceof Integer) {
            return ((Integer) empId).longValue();
        }
        return (Long) empId;
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 从 Token 中获取角色
     *
     * @param token JWT Token
     * @return 角色列表字符串
     */
    public String getRoles(String token) {
        Claims claims = parseToken(token);
        return (String) claims.get("roles");
    }

    /**
     * 获取 Token 过期时间
     *
     * @param token JWT Token
     * @return 过期时间
     */
    public Date getExpiration(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }

    /**
     * 判断 Token 是否过期
     *
     * @param claims Claims
     * @return 是否过期
     */
    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    /**
     * 判断 Token 是否可以刷新
     * 在过期前 refreshHours 小时内可以刷新
     *
     * @param token JWT Token
     * @return 是否可以刷新
     */
    public boolean canRefresh(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            Date refreshThreshold = new Date(expiration.getTime() - jwtConfig.getRefreshMillis());
            return new Date().after(refreshThreshold) && !isTokenExpired(claims);
        } catch (AuthException e) {
            return false;
        }
    }

    /**
     * 刷新 Token
     *
     * @param token 原Token
     * @return 新Token
     */
    public String refreshToken(String token) {
        Claims claims = parseToken(token);
        Long empId = getEmpId(token);
        String username = getUsername(token);
        String roles = getRoles(token);
        return generateToken(empId, username, roles);
    }

    /**
     * 获取 Token 前缀
     */
    public String getTokenPrefix() {
        return jwtConfig.getTokenPrefix();
    }

    /**
     * 获取 Token 请求头名称
     */
    public String getHeaderName() {
        return jwtConfig.getHeader();
    }
}