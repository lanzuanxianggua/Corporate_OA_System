package cn.oa.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private static final long EXPIRATION = 7200L;
    private static final long REFRESH_EXPIRATION = 604800L;
    private static final String DEFAULT_SECRET_WARNING = "default-secret-key-for-development-only";

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret:default-secret-key-for-development-only-!!}") String secret) {
        if (secret.contains(DEFAULT_SECRET_WARNING)) {
            LoggerFactory.getLogger(JwtUtil.class)
                    .warn("JWT is using the default secret. Configure jwt.secret in production.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long empId, String empName) {
        return buildToken(empId, empName, EXPIRATION);
    }

    public String generateRefreshToken(Long empId, String empName) {
        return buildToken(empId, empName, REFRESH_EXPIRATION);
    }

    private String buildToken(Long empId, String empName, long expirationSeconds) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationSeconds * 1000);
        return Jwts.builder()
                .claim("empId", empId)
                .claim("empName", empName)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
