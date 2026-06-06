package cn.oa.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应 VO.
 */
@Data
@Schema(description = "登录响应")
public class LoginResp {

    @Schema(description = "访问 Token")
    private String accessToken;

    @Schema(description = "刷新 Token")
    private String refreshToken;

    @Schema(description = "访问 Token 过期时间(秒)")
    private long expiresIn;

    @Schema(description = "Token 类型")
    private String tokenType = "Bearer";

    @Schema(description = "当前用户信息")
    private UserInfoVO userInfo;
}
