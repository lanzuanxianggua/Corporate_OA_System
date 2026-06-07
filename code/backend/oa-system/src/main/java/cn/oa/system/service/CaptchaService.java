package cn.oa.system.service;

import cn.oa.system.vo.CaptchaResp;

/**
 * 图形验证码服务.
 *
 * <p>职责:
 * <ul>
 *   <li>{@link #generate()} 生成图形验证码, 写入 Redis (key = UUID, value = 答案), 返回给前端</li>
 *   <li>{@link #validate(String, String)} 校验用户提交, 校验通过后删除 Redis 记录 (一次性)</li>
 *   <li>{@link #getExpireSeconds()} 返回当前配置的过期秒数, 供前端 UI 倒计时</li>
 * </ul>
 *
 * <p>v2 Phase 2 范围:
 * <ul>
 *   <li>使用 easy-captcha 生成 PNG, 序列化为 Base64</li>
 *   <li>Redis 存储明文小写答案 (Phase 2 简化, 后续可加 BCrypt / AES)</li>
 *   <li>校验为 GET-then-DELETE (非 Lua 原子, 留 TODO 给 c/d 子任务)</li>
 *   <li>不做限流 / 滑块 / 拼图 (c 子任务职责)</li>
 * </ul>
 *
 * <p>线程安全: 单例 Spring Bean, 所有方法无状态, 依赖 {@code StringRedisTemplate} 由 Spring 注入.
 */
public interface CaptchaService {

    /**
     * 生成图形验证码, 返回 key + Base64 图片.
     *
     * <p>Redis 中以 {@code captcha:<uuid>} 为 key, 答案为 value, TTL = {@link #getExpireSeconds()}.
     *
     * @return 含 captchaKey / imgBase64 / expiresIn 的响应
     */
    CaptchaResp generate();

    /**
     * 校验并立即失效.
     *
     * <p>校验规则:
     * <ol>
     *   <li>captchaKey / captchaCode 为空 → 抛 {@code CAPTCHA_INVALID}</li>
     *   <li>Redis 中无对应 key → 抛 {@code CAPTCHA_EXPIRED}</li>
     *   <li>value 不匹配 (忽略大小写) → 抛 {@code CAPTCHA_INVALID}</li>
     *   <li>校验通过 → 立即 DELETE key (一次性), 正常返回</li>
     * </ol>
     *
     * <p>异常一律抛出 {@code AuthDomainException} (BAD_REQUEST 子类),
     * 由 {@code GlobalExceptionHandler} 统一包装为 {@code R<>&gt;}.
     *
     * @param captchaKey   前端回传的 key
     * @param captchaCode  用户输入的字符
     */
    void validate(String captchaKey, String captchaCode);

    /**
     * 验证码过期秒数 (供前端倒计时展示).
     */
    int getExpireSeconds();
}
