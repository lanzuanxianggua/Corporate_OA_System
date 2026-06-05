package cn.oa.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "发送消息请求")
public class MsgSendDTO {
    @NotBlank @Schema(description = "消息标题")
    private String title;
    @NotBlank @Schema(description = "消息内容")
    private String content;
    @NotBlank @Schema(description = "消息类型: SYSTEM/APPROVAL/TASK")
    private String type;
    @NotNull @Schema(description = "接收人emp_id列表")
    private List<Long> recipientIds;
}