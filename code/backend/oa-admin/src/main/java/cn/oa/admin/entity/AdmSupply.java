package cn.oa.admin.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("adm_supply")
public class AdmSupply extends BaseEntity {
    private String supplyCode;
    private String supplyName;
    private Long categoryId;
    private String unit;
    private String spec;
    private Integer safetyStock;
    private String status;
}
