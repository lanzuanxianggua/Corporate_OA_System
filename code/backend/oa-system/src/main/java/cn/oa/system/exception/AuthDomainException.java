package cn.oa.system.exception;

import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;

/**
 * 认证域业务异常.
 *
 * <p>v2 Phase 1 简化: 复用现有 RCode 枚举 (不扩展 10010+ 新码),
 * 通过 message 文案区分错误原因. 后续阶段可单独扩展 RCode.
 */
public class AuthDomainException extends BizException {

    private static final long serialVersionUID = 1L;

    public AuthDomainException(RCode rCode, String message) {
        super(rCode, message);
    }

    public AuthDomainException(Integer code, String message) {
        super(code, message);
    }

    /** 用户不存在 (消息统一防枚举). */
    public static AuthDomainException userNotFound() {
        return new AuthDomainException(RCode.UNAUTHORIZED, "用户名或密码错误");
    }

    /** 密码错误. */
    public static AuthDomainException passwordInvalid() {
        return new AuthDomainException(RCode.UNAUTHORIZED, "用户名或密码错误");
    }

    /** 账号已停用. */
    public static AuthDomainException accountDisabled() {
        return new AuthDomainException(RCode.FORBIDDEN, "账号已停用");
    }

    /** 账号已锁定. */
    public static AuthDomainException accountLocked() {
        return new AuthDomainException(RCode.FORBIDDEN, "账号已锁定, 请稍后再试");
    }

    /** 验证码错误. */
    public static AuthDomainException captchaInvalid() {
        return new AuthDomainException(RCode.BAD_REQUEST, "验证码错误");
    }

    /** 验证码已过期. */
    public static AuthDomainException captchaExpired() {
        return new AuthDomainException(RCode.BAD_REQUEST, "验证码已过期, 请刷新");
    }

    /** Token 已过期. */
    public static AuthDomainException tokenExpired() {
        return new AuthDomainException(RCode.TOKEN_EXPIRED, "Token 已过期");
    }

    /** Token 无效. */
    public static AuthDomainException tokenInvalid() {
        return new AuthDomainException(RCode.INVALID_TOKEN, "Token 无效");
    }
}
