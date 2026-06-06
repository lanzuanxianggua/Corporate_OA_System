package cn.oa.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求 DTO.
 */
@Data
@Schema(description = "登录请求")
public class LoginReq {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64")
    @Schema(description = "用户名", example = "admin")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度必须在 8-64 之间")
    @Schema(description = "密码", example = "admin123")
    private String password;

    @Schema(description = "设备指纹 / 客户端 ID, 可选")
    private String clientId;
}
