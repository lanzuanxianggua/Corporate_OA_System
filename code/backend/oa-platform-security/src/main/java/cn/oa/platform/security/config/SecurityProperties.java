package cn.oa.platform.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全模块配置.
 */
@ConfigurationProperties(prefix = "oa.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private RateLimit rateLimit = new RateLimit();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }

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
