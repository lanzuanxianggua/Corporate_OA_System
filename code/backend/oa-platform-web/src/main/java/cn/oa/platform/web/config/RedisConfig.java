package cn.oa.platform.web.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置.
 *
 * <p>声明两个 Bean:
 * <ul>
 *   <li>{@link StringRedisTemplate} - 字符串场景, 验证码 key/value 都是 String</li>
 *   <li>{@link RedisTemplate}&lt;String,String&gt; - 通用 String 序列化, 避免 JDK 默认序列化
 *       在 Redis CLI 看到 {@code \xAC\xED\x00\x05t...} 这种二进制乱码</li>
 * </ul>
 *
 * <p>序列化策略: 全部使用 {@link StringRedisSerializer}, key 与 value 一致.
 * 这样在 Redis 客户端调试时可直接看到明文. 业务层 (CaptchaService 等) 自行负责
 * 把复杂对象序列化为 JSON / Base64 后再写入.
 *
 * <p>重要: 此配置仅在 {@link RedisConnectionFactory} Bean 存在时才生效.
 * 测试 profile (application-test.yml) 显式 exclude 了 RedisAutoConfiguration,
 * 此时没有 ConnectionFactory, 本配置的所有 Bean 都不会被注册, 整个
 * CaptchaServiceImpl 链路也不会被装配, AuthService 拿到 captchaService=null,
 * login() 跳过 captcha 校验, 28+ 单测零回归保持.
 *
 * <p>注意: 切面 (AOP) / 事务 / 拦截器 等不在此处配置, 由各自模块按需引入.
 */
@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisConfig {

    /**
     * 通用 String 序列化 RedisTemplate.
     *
     * <p>key / hashKey / value / hashValue 全部 {@link StringRedisSerializer},
     * 业务写入时把对象自行 JSON 化.
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 字符串场景专用模板 (captcha key/value 等), Spring Boot 默认也会提供一个,
     * 这里显式覆盖以统一序列化器, 避免与上面的 {@link #redisTemplate} 行为不一致.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
