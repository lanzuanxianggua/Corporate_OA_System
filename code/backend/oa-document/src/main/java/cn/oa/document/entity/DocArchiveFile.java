package cn.oa.document.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 档案附件.
 *
 * <p>对应表 doc_archive_files.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_archive_files")
@Schema(description = "档案附件")
public class DocArchiveFile extends BaseEntity {

    @Schema(description = "档案 ID")
    @TableField("archive_id")
    private Long archiveId;

    @Schema(description = "文件名称")
    @TableField("file_name")
    private String fileName;

    @Schema(description = "文件路径")
    @TableField("file_path")
    private String filePath;

    @Schema(description = "文件大小(字节)")
    @TableField("file_size")
    private Long fileSize;

    @Schema(description = "文件类型")
    @TableField("file_type")
    private String fileType;

    @Schema(description = "排序号")
    @TableField("sort_order")
    private Integer sortOrder;
}
