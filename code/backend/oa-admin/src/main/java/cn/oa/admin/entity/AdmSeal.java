package cn.oa.admin.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 印章管理.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("adm_seal")
@Schema(description = "印章管理")
public class AdmSeal extends BaseEntity {

    @Schema(description = "印章名称")
    private String sealName;

    @Schema(description = "类型: OFFICIAL/CONTRACT/FINANCE/PERSONAL")
    private String sealType;

    @Schema(description = "保管人ID")
    private Long custodian;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "状态: ACTIVE/INACTIVE")
    private String status;

    @Schema(description = "存放位置")
    private String location;
}
