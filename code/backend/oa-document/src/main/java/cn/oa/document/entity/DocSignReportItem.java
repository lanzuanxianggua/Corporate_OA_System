package cn.oa.document.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 签报明细.
 *
 * <p>对应表 doc_sign_report_items.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_sign_report_items")
@Schema(description = "签报明细")
public class DocSignReportItem extends BaseEntity {

    @Schema(description = "签报 ID")
    @TableField("report_id")
    private Long reportId;

    @Schema(description = "项目名称")
    @TableField("item_name")
    private String itemName;

    @Schema(description = "项目内容")
    @TableField("item_content")
    private String itemContent;

    @Schema(description = "排序号")
    @TableField("sort_order")
    private Integer sortOrder;
}
