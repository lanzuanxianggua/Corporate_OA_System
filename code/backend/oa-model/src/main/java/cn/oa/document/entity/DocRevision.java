package cn.oa.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("doc_revision")
public class DocRevision {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dispatchId;

    private Integer versionNo;

    private String content;

    private Long editorId;

    private LocalDateTime editTime;

    private String comment;

    private Boolean isClean;
}