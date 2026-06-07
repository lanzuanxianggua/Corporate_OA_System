package cn.oa.platform.security.password;

/**
 * 平台自研密码编码器接口.
 *
 * <p>设计动机: 业务模块只依赖此接口, 后续切换 Argon2 / SCrypt / PBKDF2
 * 仅替换实现类即可, 不影响业务调用方.
 *
 * <p>接口签名与 Spring 官方 {@code org.springframework.security.crypto.password.PasswordEncoder}
 * 保持一致, 便于使用方零成本迁移.
 */
public interface PasswordEncoder {

    /**
     * 对原始密码进行单向哈希, 返回密文.
     *
     * @param rawPassword 原始密码 (明文)
     * @return BCrypt 哈希字符串 (60 字符, 以 {@code $2a$}/{@code $2b$}/{@code $2y$} 开头)
     * @throws IllegalArgumentException 当 {@code rawPassword} 为 null 或空串
     */
    String encode(CharSequence rawPassword);

    /**
     * 校验原始密码与已存密文是否匹配.
     *
     * @param rawPassword       原始密码 (明文)
     * @param encodedPassword   已存的哈希字符串
     * @return 匹配返回 true; 不匹配 / 入参任一为 null 返回 false
     */
    boolean matches(CharSequence rawPassword, String encodedPassword);

    /**
     * 判断已存密文是否需要重新哈希 (如 cost 因子提升).
     *
     * @param encodedPassword 已存的哈希字符串
     * @return 当 cost 因子低于当前配置时返回 true
     */
    default boolean upgradeEncoding(String encodedPassword) {
        return false;
    }
}
