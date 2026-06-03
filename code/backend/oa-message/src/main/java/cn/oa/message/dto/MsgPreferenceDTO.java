package cn.oa.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 消息偏好更新DTO
 */
@Data
public class MsgPreferenceDTO {

    @NotNull(message = "员工ID不能为空")
    private Long empId;

    @NotBlank(message = "消息类型不能为空")
    private String msgType;

    /** 接收渠道 JSON数组 ["SITE","EMAIL"] */
    private String channels;

    /** 是否启用 0-禁用 1-启用 */
    private Integer enabled;
}
