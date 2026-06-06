package cn.oa.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图形验证码响应 VO.
 */
@Data
@Schema(description = "图形验证码响应")
public class CaptchaResp {

    @Schema(description = "验证码 Key, 一次性 UUID, 提交登录时回传")
    private String captchaKey;

    @Schema(description = "验证码图片 (Base64 编码, 不含 data:image/png;base64, 前缀)")
    private String imgBase64;

    @Schema(description = "过期时间(秒)")
    private int expiresIn = 300;
}
