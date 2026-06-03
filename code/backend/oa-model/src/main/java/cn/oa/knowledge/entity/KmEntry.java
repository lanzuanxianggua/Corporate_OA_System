package cn.oa.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识条目
 */
@Data
@TableName("km_entry")
public class KmEntry {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private Integer currentVersion;

    /** DRAFT-草稿 PUBLISHED-已发布 ARCHIVED-已归档 */
    private String status;

    private Long deptId;

    private String securityLevel;

    private Long categoryId;

    private Integer viewCount;

    private Integer downloadCount;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
