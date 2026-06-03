package cn.oa.platform.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置
 *
 * @author oa-platform
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * 密钥（至少256位，建议32字符以上）
     */
    private String secret = "oa-platform-secret-key-2024-default-must-change-in-production";

    /**
     * Token 过期时间（小时）
     */
    private int expireHours = 2;

    /**
     * Token 刷新时间（小时）- 在此时间内可以刷新 Token
     */
    private int refreshHours = 1;

    /**
     * Token 前缀
     */
    private String tokenPrefix = "Bearer ";

    /**
     * Token 请求头名称
     */
    private String header = "Authorization";

    /**
     * Issuer（签发者）
     */
    private String issuer = "oa-platform";

    /**
     * Audience（接收者）
     */
    private String audience = "oa-user";

    /**
     * 获取过期时间（毫秒）
     */
    public long getExpireMillis() {
        return expireHours * 60 * 60 * 1000L;
    }

    /**
     * 获取刷新时间（毫秒）
     */
    public long getRefreshMillis() {
        return refreshHours * 60 * 60 * 1000L;
    }
}