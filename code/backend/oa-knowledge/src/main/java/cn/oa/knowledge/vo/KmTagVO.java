package cn.oa.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 知识标签 VO
 */
@Data
@Schema(description = "知识标签")
public class KmTagVO {

    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "标签类型(CUSTOM/SYSTEM/CATEGORY)")
    private String tagType;

    @Schema(description = "使用次数")
    private Integer usageCount;
}
