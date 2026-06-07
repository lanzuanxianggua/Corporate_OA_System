package cn.oa.platform.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全模块配置.
 */
@ConfigurationProperties(prefix = "oa.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private RateLimit rateLimit = new RateLimit();
    private Bcrypt bcrypt = new Bcrypt();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }
    public Bcrypt getBcrypt() { return bcrypt; }
    public void setBcrypt(Bcrypt bcrypt) { this.bcrypt = bcrypt; }

    /**
     * BCrypt 密码编码器配置.
     */
    public static class Bcrypt {
        /** BCrypt cost 因子, 范围 [4, 31], 默认 10. */
        private int strength = 10;
        /**
         * 登录路径上检测到明文旧密码时是否自动升级为 BCrypt 哈希.
         * <p>默认开启, 保证存量 V910 明文 seed 用户首次登录后自动迁移.
         */
        private boolean enableLazyRehash = true;

        public int getStrength() { return strength; }
        public void setStrength(int strength) { this.strength = strength; }
        public boolean isEnableLazyRehash() { return enableLazyRehash; }
        public void setEnableLazyRehash(boolean enableLazyRehash) { this.enableLazyRehash = enableLazyRehash; }
    }

    public static class Jwt {
        private String secret = "oa-default-secret-key-please-change-in-production-32+chars";
        private long accessTtlSeconds = 7200;
        private long refreshTtlSeconds = 604800;
        private String issuer = "oa-system";
        private String header = "Authorization";
        private String prefix = "Bearer ";

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getAccessTtlSeconds() { return accessTtlSeconds; }
        public void setAccessTtlSeconds(long s) { this.accessTtlSeconds = s; }
        public long getRefreshTtlSeconds() { return refreshTtlSeconds; }
        public void setRefreshTtlSeconds(long s) { this.refreshTtlSeconds = s; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getHeader() { return header; }
        public void setHeader(String header) { this.header = header; }
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
    }

    public static class RateLimit {
        private boolean enabled = true;
        private int defaultCapacity = 100;
        private int defaultRefill = 10;
        private int defaultDuration = 60;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getDefaultCapacity() { return defaultCapacity; }
        public void setDefaultCapacity(int c) { this.defaultCapacity = c; }
        public int getDefaultRefill() { return defaultRefill; }
        public void setDefaultRefill(int r) { this.defaultRefill = r; }
        public int getDefaultDuration() { return defaultDuration; }
        public void setDefaultDuration(int d) { this.defaultDuration = d; }
    }
}
