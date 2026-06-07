package cn.oa.system.service.impl;

import cn.oa.system.exception.AuthDomainException;
import cn.oa.system.service.CaptchaService;
import cn.oa.system.vo.CaptchaResp;
import com.wf.captcha.SpecCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

/**
 * 图形验证码服务实现.
 *
 * <p>流程:
 * <ol>
 *   <li>{@link #generate()}: SpecCaptcha 生成 PNG → Base64 (去除 data: 前缀), UUID 作为 key
 *       → Redis {@code captcha:<uuid>} = 小写答案, TTL = 5 分钟 → 返回 {@link CaptchaResp}</li>
 *   <li>{@link #validate(String, String)}: GET-then-DELETE:
 *       先从 Redis 读取, 命中且忽略大小写匹配 → 立即 DELETE (一次性) → 通过;
 *       未命中 → 抛 {@code CAPTCHA_EXPIRED}; 不匹配 → 抛 {@code CAPTCHA_INVALID}.
 *       (非原子, 留 TODO 后续切 Lua)</li>
 * </ol>
 *
 * <p>关于 RedisTemplate 不可用 (测试环境排除 RedisAutoConfiguration): 调用方
 * (AuthController 注入的 captchaService) 应当允许 {@code @Autowired(required=false)}
 * 或在 login 路径上做 null 判定. 本类不做 null 防御, 由调用方负责.
 */
@Slf4j
@Service
@ConditionalOnBean(StringRedisTemplate.class)
public class CaptchaServiceImpl implements CaptchaService {

    /** Redis key 前缀. */
    static final String KEY_PREFIX = "captcha:";
    /** 默认过期时间: 5 分钟. */
    static final int DEFAULT_EXPIRE_SECONDS = 300;
    /** SpecCaptcha 宽高 (像素). */
    private static final int CAPTCHA_WIDTH = 130;
    private static final int CAPTCHA_HEIGHT = 48;
    /** 验证码字符长度 (4 位纯字母/数字). */
    private static final int CAPTCHA_LEN = 4;

    private final StringRedisTemplate redisTemplate;
    /** 暴露给前端的过期秒数 (可外部配置覆盖, 当前固定 300s). */
    private final int expireSeconds;

    @Autowired
    public CaptchaServiceImpl(StringRedisTemplate redisTemplate) {
        this(redisTemplate, DEFAULT_EXPIRE_SECONDS);
    }

    /** 构造器 (供单测传自定义 TTL). */
    public CaptchaServiceImpl(StringRedisTemplate redisTemplate, int expireSeconds) {
        this.redisTemplate = redisTemplate;
        this.expireSeconds = expireSeconds;
    }

    @Override
    public CaptchaResp generate() {
        // 1) 生成图形
        SpecCaptcha captcha = new SpecCaptcha(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, CAPTCHA_LEN);
        captcha.setCharType(SpecCaptcha.TYPE_DEFAULT); // 字母 + 数字混合
        String answer = captcha.text();                  // 正确文本 (4 位)
        String base64 = captcha.toBase64();              // data:image/png;base64,xxxx
        // 去掉前缀, 前端按需拼接或直接展示
        String imgBase64 = stripDataUriPrefix(base64);

        // 2) 写 Redis
        String key = KEY_PREFIX + UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(key, answer.toLowerCase(Locale.ROOT),
                Duration.ofSeconds(expireSeconds));

        // 3) 组装响应
        CaptchaResp resp = new CaptchaResp();
        resp.setCaptchaKey(key);
        resp.setImgBase64(imgBase64);
        resp.setExpiresIn(expireSeconds);
        log.debug("captcha generated: key={}, answer(len)={}", key, answer.length());
        return resp;
    }

    @Override
    public void validate(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaKey.isBlank()
                || captchaCode == null || captchaCode.isBlank()) {
            throw AuthDomainException.captchaInvalid();
        }
        if (!captchaKey.startsWith(KEY_PREFIX)) {
            // 防止 Redis 误删其他业务 key
            throw AuthDomainException.captchaInvalid();
        }

        String expected = redisTemplate.opsForValue().get(captchaKey);
        if (expected == null) {
            throw AuthDomainException.captchaExpired();
        }
        if (!expected.equals(captchaCode.trim().toLowerCase(Locale.ROOT))) {
            throw AuthDomainException.captchaInvalid();
        }

        // 一次性: 校验通过立即删除, 防止重放
        Boolean deleted = redisTemplate.delete(captchaKey);
        if (Boolean.FALSE.equals(deleted)) {
            // 极端竞态: 已被另一线程删除 → 视为过期
            log.warn("captcha key disappeared between GET and DELETE: {}", captchaKey);
            throw AuthDomainException.captchaExpired();
        }
        log.debug("captcha validated: key={}", captchaKey);
    }

    @Override
    public int getExpireSeconds() {
        return expireSeconds;
    }

    private static String stripDataUriPrefix(String dataUri) {
        int idx = dataUri.indexOf(',');
        return idx < 0 ? dataUri : dataUri.substring(idx + 1);
    }
}
