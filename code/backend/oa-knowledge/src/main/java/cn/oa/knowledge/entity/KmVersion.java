package cn.oa.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识版本实体
 */
@Data
@TableName("km_version")
public class KmVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long entryId;

    private Integer versionNo;

    private String filePath;

    private Long fileSize;

    private String fileType;

    private String fileHash;

    private Long uploaderId;

    private LocalDateTime uploadTime;

    private String comment;
}
