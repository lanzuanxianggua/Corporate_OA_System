package cn.oa.platform.security.password;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BCryptPasswordEncoder 单元测试.
 *
 * <p>覆盖 encode / matches / upgradeEncoding / 边界 null 与 strength 越界.
 */
class BCryptPasswordEncoderTest {

    @Test
    @DisplayName("encode 产生 60 字符的 BCrypt 哈希, 以 $2a$ 开头")
    void encode_producesValidHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        String hash = encoder.encode("admin123");

        assertThat(hash).hasSize(60);
        assertThat(hash).startsWith("$2a$04$");
    }

    @Test
    @DisplayName("matches: encode 后的明文与哈希匹配返回 true")
    void matches_true() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        String hash = encoder.encode("admin123");

        assertThat(encoder.matches("admin123", hash)).isTrue();
    }

    @Test
    @DisplayName("matches: 错误明文返回 false")
    void matches_false() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        String hash = encoder.encode("admin123");

        assertThat(encoder.matches("wrong-pwd", hash)).isFalse();
    }

    @Test
    @DisplayName("matches: encodedPassword 为 null 返回 false (不抛异常)")
    void matches_nullEncoded_false() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

        assertThat(encoder.matches("admin123", null)).isFalse();
    }

    @Test
    @DisplayName("matches: rawPassword 为 null 返回 false (不抛异常)")
    void matches_nullRaw_false() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        String hash = encoder.encode("admin123");

        assertThat(encoder.matches(null, hash)).isFalse();
    }

    @Test
    @DisplayName("matches: 非 BCrypt 格式的 encodedPassword 返回 false (不抛异常)")
    void matches_nonBcryptFormat_false() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

        assertThat(encoder.matches("admin123", "plain-text")).isFalse();
        assertThat(encoder.matches("admin123", "")).isFalse();
        assertThat(encoder.matches("admin123", "$1$abc")).isFalse();
    }

    @Test
    @DisplayName("encode 两次结果不同 (salt 随机)")
    void encode_saltRandom() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        String hash1 = encoder.encode("admin123");
        String hash2 = encoder.encode("admin123");

        assertThat(hash1).isNotEqualTo(hash2);
        // 但都应能匹配
        assertThat(encoder.matches("admin123", hash1)).isTrue();
        assertThat(encoder.matches("admin123", hash2)).isTrue();
    }

    @Test
    @DisplayName("encode null 抛 IllegalArgumentException")
    void encode_null_throws() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

        assertThatThrownBy(() -> encoder.encode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("encode 空串抛 IllegalArgumentException")
    void encode_empty_throws() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

        assertThatThrownBy(() -> encoder.encode(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    @DisplayName("upgradeEncoding: 低 cost 哈希在高 strength encoder 下需要重 hash")
    void upgradeEncoding_lowerCost_true() {
        BCryptPasswordEncoder encoder10 = new BCryptPasswordEncoder(10);
        // 构造一个 cost=4 的 BCrypt 哈希
        String lowCostHash = new BCryptPasswordEncoder(4).encode("admin123");

        assertThat(lowCostHash).startsWith("$2a$04$");
        assertThat(encoder10.upgradeEncoding(lowCostHash)).isTrue();
    }

    @Test
    @DisplayName("upgradeEncoding: 相同 cost 哈希不需要重 hash")
    void upgradeEncoding_sameCost_false() {
        BCryptPasswordEncoder encoder4 = new BCryptPasswordEncoder(4);
        String hash = encoder4.encode("admin123");

        assertThat(encoder4.upgradeEncoding(hash)).isFalse();
    }

    @Test
    @DisplayName("upgradeEncoding: null / 非 BCrypt 格式返回 false")
    void upgradeEncoding_invalid_false() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

        assertThat(encoder.upgradeEncoding(null)).isFalse();
        assertThat(encoder.upgradeEncoding("plain-text")).isFalse();
        assertThat(encoder.upgradeEncoding("")).isFalse();
    }

    @Test
    @DisplayName("strength 越界 (3 或 32) 抛 IllegalArgumentException")
    void strength_outOfRange_throws() {
        assertThatThrownBy(() -> new BCryptPasswordEncoder(3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BCrypt strength");
        assertThatThrownBy(() -> new BCryptPasswordEncoder(32))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BCrypt strength");
    }

    @Test
    @DisplayName("strength 边界 4 / 31 可正常构造")
    void strength_boundary_ok() {
        assertThat(new BCryptPasswordEncoder(4).getStrength()).isEqualTo(4);
        assertThat(new BCryptPasswordEncoder(31).getStrength()).isEqualTo(31);
    }

    @Test
    @DisplayName("默认构造器 strength=10")
    void defaultStrength_is10() {
        assertThat(new BCryptPasswordEncoder().getStrength()).isEqualTo(10);
    }

    @Test
    @DisplayName("looksHashed: 正确识别 $2a$/$2b$/$2y$ 开头")
    void looksHashed_patterns() {
        assertThat(BCryptPasswordEncoder.looksHashed("$2a$10$abc")).isTrue();
        assertThat(BCryptPasswordEncoder.looksHashed("$2b$10$abc")).isTrue();
        assertThat(BCryptPasswordEncoder.looksHashed("$2y$10$abc")).isTrue();
        assertThat(BCryptPasswordEncoder.looksHashed("plain-text")).isFalse();
        assertThat(BCryptPasswordEncoder.looksHashed("$1$abc")).isFalse();
        assertThat(BCryptPasswordEncoder.looksHashed(null)).isFalse();
        assertThat(BCryptPasswordEncoder.looksHashed("")).isFalse();
        assertThat(BCryptPasswordEncoder.looksHashed("$2a")).isFalse();
    }
}
