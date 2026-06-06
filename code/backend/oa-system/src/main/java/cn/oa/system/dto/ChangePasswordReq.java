package cn.oa.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求 DTO.
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordReq {

    @NotBlank(message = "原密码不能为空")
    @Schema(description = "原密码")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "新密码长度必须在 8-32 之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "新密码必须同时包含字母与数字")
    @Schema(description = "新密码")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码")
    private String confirmPassword;
}
