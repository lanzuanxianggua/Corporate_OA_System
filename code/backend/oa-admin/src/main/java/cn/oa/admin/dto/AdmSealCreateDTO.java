package cn.oa.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 印章创建/修改 DTO.
 */
@Data
@Schema(description = "印章创建/修改请求")
public class AdmSealCreateDTO {

    @NotBlank(message = "印章名称不能为空")
    @Schema(description = "印章名称")
    private String sealName;

    @NotBlank(message = "印章类型不能为空")
    @Schema(description = "类型: OFFICIAL/CONTRACT/FINANCE/PERSONAL")
    private String sealType;

    @NotNull(message = "保管人不能为空")
    @Schema(description = "保管人ID")
    private Long custodian;

    @NotNull(message = "所属部门不能为空")
    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "存放位置")
    private String location;
}
