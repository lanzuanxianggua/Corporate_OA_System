package cn.oa.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Schema(description = "知识条目")
public class KmEntryVO {

    private Long id; private String title; private String content; private String summary;

    private Long categoryId; private String categoryName; private String tags;

    private String status; private Integer viewCount; private Integer versionNo;

    private String createEmpName; private LocalDateTime createTime;
}