package cn.oa.document.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 档案.
 *
 * <p>对应表 doc_archives.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_archives")
@Schema(description = "档案")
public class DocArchive extends BaseEntity {

    @Schema(description = "档案编号")
    @TableField("archive_no")
    private String archiveNo;

    @Schema(description = "档案标题")
    @TableField("archive_title")
    private String archiveTitle;

    @Schema(description = "文档类型: DISPATCH/RECEIVE/SIGN_REPORT")
    @TableField("doc_type")
    private String docType;

    @Schema(description = "关联业务 ID")
    @TableField("biz_id")
    private Long bizId;

    @Schema(description = "档案备注")
    @TableField("remark")
    private String remark;

    @Schema(description = "档案状态: ACTIVE/FROZEN/DESTROYED")
    @TableField("status")
    private String status;
}
