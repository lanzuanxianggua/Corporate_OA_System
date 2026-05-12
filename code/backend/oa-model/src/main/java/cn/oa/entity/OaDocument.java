package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_document")
public class OaDocument {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String docName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    private Long categoryId;

    private Long uploaderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
