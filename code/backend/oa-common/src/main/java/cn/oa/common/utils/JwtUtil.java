package cn.oa.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
public class JwtUtil {

    /** 密钥（必须>=256位即32字节，这里用48字节） */
    private static final SecretKey SIGNING_KEY =
            Keys.hmacShaKeyFor("oa-system-jwt-secret-key-2024-secure!!".getBytes(StandardCharsets.UTF_8));

    /** Token 有效期（秒） */
    private static final long EXPIRATION = 7200L;

    /**
     * 生成 Token
     */
    public static String generateToken(Long empId, String empName) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION * 1000);

        return Jwts.builder()
                .claim("empId", empId)
                .claim("empName", empName)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(SIGNING_KEY, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析 Token
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(SIGNING_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
