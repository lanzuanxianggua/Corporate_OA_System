package cn.oa.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
@Schema(description = "未读消息数")
public class MsgUnreadCountVO {
    private Long total;
}