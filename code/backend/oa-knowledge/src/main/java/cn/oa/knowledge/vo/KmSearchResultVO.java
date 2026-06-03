package cn.oa.knowledge.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识搜索结果 VO
 */
@Data
@Schema(description = "知识搜索结果")
public class KmSearchResultVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容摘要")
    private String summary;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "浏览次数")
    private Integer viewCount;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "相关性得分")
    private Double score;
}
