package cn.oa.knowledge.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 版本历史.
 *
 * <p>每次编辑知识条目时自动生成一条版本快照.
 * entry_id + version_no 唯一索引.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("km_versions")
@Schema(description = "版本历史")
public class KmVersion extends BaseEntity {

    @Schema(description = "知识条目ID")
    @TableField("entry_id")
    private Long entryId;

    @Schema(description = "版本号")
    @TableField("version_no")
    private Integer versionNo;

    @Schema(description = "标题(快照)")
    @TableField("title")
    private String title;

    @Schema(description = "正文(快照)")
    @TableField("content")
    private String content;

    @Schema(description = "摘要(快照)")
    @TableField("summary")
    private String summary;

    @Schema(description = "变更说明")
    @TableField("change_note")
    private String changeNote;

    @Schema(description = "创建人emp_id")
    @TableField("create_emp_id")
    private Long createEmpId;
}
