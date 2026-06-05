package cn.oa.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 印章 VO.
 */
@Data
@Schema(description = "印章视图对象")
public class AdmSealVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "印章名称")
    private String sealName;

    @Schema(description = "类型")
    private String sealType;

    @Schema(description = "保管人ID")
    private Long custodian;

    @Schema(description = "保管人姓名")
    private String custodianName;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "所属部门名称")
    private String deptName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "存放位置")
    private String location;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
