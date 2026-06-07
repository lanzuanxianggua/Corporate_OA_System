package cn.oa.platform.security.password;

import lombok.Getter;
import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * 基于 Spring {@code spring-security-crypto} BCrypt 算法的密码编码器.
 *
 * <p>内部代理 {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder} 实现,
 * 行为与官方实现保持一致:
 * <ul>
 *   <li>默认 strength=10 (cost 因子, 范围 [4, 31])</li>
 *   <li>{@code encode(null)} / {@code encode("")} 抛 {@link IllegalArgumentException}</li>
 *   <li>{@code matches(raw, null)} / {@code matches(null, encoded)} 返回 false (不抛异常)</li>
 *   <li>每次 encode 结果不同 (salt 随机)</li>
 * </ul>
 *
 * <p>对外仅暴露本平台 {@link PasswordEncoder} 接口, 避免业务模块直接依赖 Spring Security 类型.
 */
public class BCryptPasswordEncoder implements PasswordEncoder {

    /** 允许的最小 strength. */
    public static final int MIN_STRENGTH = 4;
    /** 允许的最大 strength. */
    public static final int MAX_STRENGTH = 31;

    @Getter
    private final int strength;

    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder delegate;

    /**
     * 构造默认强度 (10) 的编码器.
     */
    public BCryptPasswordEncoder() {
        this(10);
    }

    /**
     * 构造指定强度的编码器.
     *
     * @param strength BCrypt cost 因子, 范围 [4, 31]
     * @throws IllegalArgumentException 当 strength 越界
     */
    public BCryptPasswordEncoder(int strength) {
        if (strength < MIN_STRENGTH || strength > MAX_STRENGTH) {
            throw new IllegalArgumentException(
                    "BCrypt strength must be in [" + MIN_STRENGTH + ", " + MAX_STRENGTH
                            + "], got " + strength);
        }
        this.strength = strength;
        this.delegate = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(strength);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword must not be null");
        }
        String s = rawPassword.toString();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("rawPassword must not be empty");
        }
        return delegate.encode(s);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }
        // 防御: 非 BCrypt 格式的密文直接返回 false, 避免 BCrypt 内部解析抛 IllegalArgumentException
        if (!encodedPassword.startsWith("$2") || encodedPassword.length() < 4) {
            return false;
        }
        try {
            return delegate.matches(rawPassword.toString(), encodedPassword);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        if (encodedPassword == null || !looksHashed(encodedPassword)) {
            return false;
        }
        // BCrypt 哈希格式: $2a$<cost>$<22-char-salt><31-char-hash>
        // cost 在第 4 位起的两位十进制数
        try {
            int existingCost = Integer.parseInt(encodedPassword.substring(4, 6));
            return existingCost < this.strength;
        } catch (NumberFormatException | IndexOutOfBoundsException ex) {
            return false;
        }
    }

    /**
     * 快速判定是否为 BCrypt 哈希格式 (供 AuthService 走双轨判断).
     */
    public static boolean looksHashed(String s) {
        if (s == null || s.length() < 4) {
            return false;
        }
        return s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$");
    }

    /**
     * 暴露底层 BCrypt 静态方法 (供测试 / 工具使用).
     */
    public static String hashpw(String plaintext, int strength) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(strength));
    }
}
