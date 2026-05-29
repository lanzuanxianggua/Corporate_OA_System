package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_document")
public class OaDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("title")
    private String docName;

    @TableField("content")
    private String description;

    @TableField("file_url")
    private String filePath;

    @TableField(exist = false)
    private Long fileSize;

    @TableField(exist = false)
    private String fileType;

    private Long categoryId;

    @TableField(exist = false)
    private Integer downloadCount = 0;

    @TableField("emp_id")
    private Long uploaderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
