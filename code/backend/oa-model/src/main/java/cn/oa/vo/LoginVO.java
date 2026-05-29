package cn.oa.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录返回数据
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** token */
    private String accessToken;

    /** 刷新token */
    private String refreshToken;

    /** 过期时间 (格式: yyyy/MM/dd HH:mm:ss) */
    private String expires;

    /** 用户名（员工编号） */
    private String username;

    /** 昵称（员工姓名） */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 角色列表 */
    private List<String> roles;

    /** 权限列表 */
    private List<String> permissions;
}
