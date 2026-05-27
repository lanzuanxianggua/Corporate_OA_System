package cn.oa.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求参数
 */
@Data
public class LoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 验证码UUID */
    private String captchaUuid;

    /** 验证码 */
    private String captchaCode;
}
