package cn.oa.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 档案 VO.
 */
@Data
@Schema(description = "档案详情")
public class DocArchiveVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "档案编号 (唯一)")
    private String archiveNo;

    @Schema(description = "档案类型: DISPATCH/RECEIVE/SIGN_REPORT")
    private String archiveType;

    @Schema(description = "关联业务 ID")
    private Long sourceId;

    @Schema(description = "归档日期")
    private LocalDate archiveDate;

    @Schema(description = "档案标题")
    private String title;

    @Schema(description = "档案状态: ACTIVE/FROZEN/DESTROYED")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
