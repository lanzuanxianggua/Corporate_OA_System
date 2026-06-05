package cn.oa.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "消息查询参数")
public class MsgNotificationQueryDTO {
    @Schema(description = "页码")
    private Integer pageNum = 1;
    @Schema(description = "每页大小")
    private Integer pageSize = 10;
}