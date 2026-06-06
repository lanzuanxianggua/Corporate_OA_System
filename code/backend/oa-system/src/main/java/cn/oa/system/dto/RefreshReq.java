package cn.oa.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新 Token 请求 DTO.
 */
@Data
@Schema(description = "刷新 Token 请求")
public class RefreshReq {

    @NotBlank(message = "refreshToken 不能为空")
    @Schema(description = "刷新 Token")
    private String refreshToken;
}
