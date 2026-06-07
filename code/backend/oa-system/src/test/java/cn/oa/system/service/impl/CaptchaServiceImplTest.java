package cn.oa.system.service.impl;

import cn.oa.system.exception.AuthDomainException;
import cn.oa.system.vo.CaptchaResp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CaptchaServiceImpl 单测.
 *
 * <p>v2 Phase 2: Redis 行为用 {@link StringRedisTemplate} mock, 验证:
 * <ol>
 *   <li>generate() 写 Redis, 答案小写, TTL=expireSeconds, key 前缀 captcha:</li>
 *   <li>validate() 命中 + 大小写不敏感 + 删除 key (一次性)</li>
 *   <li>validate() 找不到 key 抛 CAPTCHA_EXPIRED</li>
 *   <li>validate() 答案不匹配抛 CAPTCHA_INVALID, 不删 key</li>
 * </ol>
 */
class CaptchaServiceImplTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private CaptchaServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new CaptchaServiceImpl(redisTemplate, 300);
    }

    @Test
    @DisplayName("generate: 写 Redis, key 前缀 captcha:, 答案小写, TTL=300s, 返回 CaptchaResp")
    void generate_writesRedis() {
        CaptchaResp resp = service.generate();

        // 1) 响应非空 + 字段正确
        assertThat(resp).isNotNull();
        assertThat(resp.getCaptchaKey()).startsWith("captcha:");
        // 32 字符 UUID 去掉 -, 即 32 位 hex
        assertThat(resp.getCaptchaKey().substring("captcha:".length())).hasSize(32);
        // Base64 包含 PNG header (data:image/png;base64 去除前缀后)
        assertThat(resp.getImgBase64()).isNotEmpty();
        assertThat(resp.getExpiresIn()).isEqualTo(300);

        // 2) Redis 写入: key 前缀, 答案小写, TTL=300
        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCap = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(keyCap.capture(), valCap.capture(), ttlCap.capture());
        assertThat(keyCap.getValue()).startsWith("captcha:");
        assertThat(valCap.getValue()).hasSize(4).matches("[a-z0-9]+"); // 答案已 lowerCase
        assertThat(ttlCap.getValue()).isEqualTo(Duration.ofSeconds(300));
    }

    @Test
    @DisplayName("validate: 命中 + 忽略大小写 + 校验后 DELETE key (一次性)")
    void validate_passAndDelete() {
        // 1) 准备 Redis 状态
        String key = "captcha:abc123";
        when(valueOps.get(key)).thenReturn("abcd");  // 小写
        when(redisTemplate.delete(key)).thenReturn(true);

        // 2) 大写输入也应当匹配
        service.validate(key, "ABCD");

        // 3) 验证 DELETE 被调用
        verify(redisTemplate).delete(key);
    }

    @Test
    @DisplayName("validate: key 不存在 (Redis 返回 null) → 抛 CAPTCHA_EXPIRED, 不 DELETE")
    void validate_missingKey() {
        String key = "captcha:expired";
        when(valueOps.get(key)).thenReturn(null);

        assertThatThrownBy(() -> service.validate(key, "abcd"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessageContaining("验证码已过期");

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("validate: 答案不匹配 → 抛 CAPTCHA_INVALID, 不 DELETE (允许用户重试到过期)")
    void validate_wrongCode() {
        String key = "captcha:wrong";
        when(valueOps.get(key)).thenReturn("abcd");

        assertThatThrownBy(() -> service.validate(key, "wxyz"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessageContaining("验证码错误");

        // 不匹配时不应消耗 key, 让用户继续尝试
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("validate: 空 key / 空 code → 抛 CAPTCHA_INVALID (参数校验)")
    void validate_blankInput() {
        assertThatThrownBy(() -> service.validate(null, "abcd"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessageContaining("验证码错误");
        assertThatThrownBy(() -> service.validate("captcha:abc", ""))
                .isInstanceOf(AuthDomainException.class)
                .hasMessageContaining("验证码错误");
        assertThatThrownBy(() -> service.validate("captcha:abc", "   "))
                .isInstanceOf(AuthDomainException.class)
                .hasMessageContaining("验证码错误");
        verify(valueOps, never()).get(any());
    }

    @Test
    @DisplayName("validate: key 不带 captcha: 前缀 → 抛 CAPTCHA_INVALID (安全防护, 避免误删其他业务 key)")
    void validate_keyWithoutPrefix() {
        assertThatThrownBy(() -> service.validate("user:1:password", "abcd"))
                .isInstanceOf(AuthDomainException.class)
                .hasMessageContaining("验证码错误");

        // 关键: 不应执行任何 Redis 读 / 删
        verify(valueOps, never()).get(any());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("getExpireSeconds: 返回构造时传入的 TTL")
    void expireSeconds() {
        assertThat(service.getExpireSeconds()).isEqualTo(300);
        // 自定义 TTL 也能正确返回
        CaptchaServiceImpl custom = new CaptchaServiceImpl(redisTemplate, 60);
        assertThat(custom.getExpireSeconds()).isEqualTo(60);
    }
}
