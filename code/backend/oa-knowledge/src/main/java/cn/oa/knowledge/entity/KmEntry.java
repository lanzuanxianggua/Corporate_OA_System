package cn.oa.knowledge.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识条目实体
 */
@Data
@TableName("km_entry")
public class KmEntry {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private Integer currentVersion;

    private String status; // DRAFT/PUBLISHED/ARCHIVED

    private Long deptId;

    private String securityLevel; // PUBLIC/INTERNAL/SECRET

    private Long categoryId;

    private Integer viewCount;

    private Integer downloadCount;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
