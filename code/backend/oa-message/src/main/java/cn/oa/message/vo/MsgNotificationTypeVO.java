package cn.oa.message.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知类型字典 VO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知类型字典 VO")
public class MsgNotificationTypeVO {
    @Schema(description = "类型编码")
    private String code;

    @Schema(description = "类型名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "0=禁用 1=启用")
    private Integer enabled;

    @Schema(description = "排序")
    private Integer sortOrder;
}
