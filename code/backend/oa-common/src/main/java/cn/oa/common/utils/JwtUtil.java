package cn.oa.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
@Component
public class JwtUtil {

    /** Token 有效期（秒） */
    private static final long EXPIRATION = 7200L;

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret:default-secret-key-for-development-only-!!}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token
     */
    public String generateToken(Long empId, String empName) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION * 1000);

        return Jwts.builder()
                .claim("empId", empId)
                .claim("empName", empName)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
