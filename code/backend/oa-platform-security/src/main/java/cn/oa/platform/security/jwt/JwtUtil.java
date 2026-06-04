package cn.oa.platform.security.jwt;

import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.platform.security.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT 工具类.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final SecurityProperties.Jwt cfg;

    public JwtUtil(SecurityProperties props) {
        this.cfg = props.getJwt();
        this.key = Keys.hmacShaKeyFor(cfg.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long empId, String username, List<String> roles, List<String> permissions) {
        return generate(empId, username, roles, permissions, cfg.getAccessTtlSeconds(), "access");
    }

    public String generateRefreshToken(Long empId, String username) {
        return generate(empId, username, List.of(), List.of(), cfg.getRefreshTtlSeconds(), "refresh");
    }

    private String generate(Long empId, String username, List<String> roles,
                            List<String> permissions, long ttlSeconds, String type) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", empId);
        claims.put("uname", username);
        claims.put("roles", roles);
        claims.put("perms", permissions);
        claims.put("type", type);

        return Jwts.builder()
                .issuer(cfg.getIssuer())
                .subject(String.valueOf(empId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .claims(claims)
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(cfg.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BizException(RCode.TOKEN_EXPIRED, "Token 过期");
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(RCode.INVALID_TOKEN, "Token 无效");
        }
    }

    public UserInfo extract(Claims claims) {
        Long empId = claims.get("uid", Long.class);
        String username = claims.get("uname", String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.getOrDefault("roles", List.of());
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) claims.getOrDefault("perms", List.of());
        return new UserInfo(empId, username, roles, permissions);
    }

    public String resolveToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        String prefix = cfg.getPrefix();
        if (prefix != null && !prefix.isBlank() && authHeader.startsWith(prefix)) {
            return authHeader.substring(prefix.length()).trim();
        }
        return authHeader.trim();
    }

    public static class UserInfo {
        private final Long empId;
        private final String username;
        private final List<String> roles;
        private final List<String> permissions;

        public UserInfo(Long empId, String username, List<String> roles, List<String> permissions) {
            this.empId = empId;
            this.username = username;
            this.roles = roles;
            this.permissions = permissions;
        }

        public Long getEmpId() { return empId; }
        public String getUsername() { return username; }
        public List<String> getRoles() { return roles; }
        public List<String> getPermissions() { return permissions; }
    }
}
