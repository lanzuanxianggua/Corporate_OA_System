package cn.oa.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 档案 VO.
 */
@Data
@Schema(description = "档案详情")
public class DocArchiveVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "档案编号")
    private String archiveNo;

    @Schema(description = "档案标题")
    private String archiveTitle;

    @Schema(description = "文档类型: DISPATCH/RECEIVE/SIGN_REPORT")
    private String docType;

    @Schema(description = "关联业务 ID")
    private Long bizId;

    @Schema(description = "档案备注")
    private String remark;

    @Schema(description = "档案状态: ACTIVE/FROZEN/DESTROYED")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
