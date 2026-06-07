package cn.oa.document.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 档案.
 *
 * <p>对应表 doc_archives.
 * 状态: ACTIVE / FROZEN / DESTROYED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_archives")
@Schema(description = "档案")
public class DocArchive extends BaseEntity {

    @Schema(description = "档案编号 (唯一)")
    @TableField("archive_no")
    private String archiveNo;

    @Schema(description = "档案类型: DISPATCH/RECEIVE/SIGN_REPORT")
    @TableField("archive_type")
    private String archiveType;

    @Schema(description = "关联业务 ID (如发文 ID / 收文 ID / 签报 ID)")
    @TableField("source_id")
    private Long sourceId;

    @Schema(description = "归档日期")
    @TableField("archive_date")
    private LocalDate archiveDate;

    @Schema(description = "档案标题")
    @TableField("title")
    private String title;

    @Schema(description = "档案状态: ACTIVE/FROZEN/DESTROYED")
    @TableField("status")
    private String status;
}
