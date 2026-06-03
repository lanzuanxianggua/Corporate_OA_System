package cn.oa.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公文修订版本表
 * 对应表: doc_revision
 *
 * @author oa-document
 */
@Data
@TableName("doc_revision")
public class DocRevision {

    /** 修订ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发文ID */
    private Long dispatchId;

    /** 版本号 */
    private Integer versionNo;

    /** 正文内容/附件路径 */
    private String content;

    /** 编辑人ID */
    private Long editorId;

    /** 编辑时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime editTime;

    /** 版本备注 */
    private String comment;

    /** 是否清稿(0-否 1-是) */
    private Integer isClean;

    /** 删除标志(0存在 1删除) */
    @TableLogic
    private String delFlag;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新人 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
