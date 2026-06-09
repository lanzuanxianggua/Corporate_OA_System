package cn.oa.admin.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("adm_supply_stock")
public class AdmSupplyStock extends BaseEntity {
    private Long supplyId;
    private Integer quantity;
    private Integer lockedQuantity;
    private String location;
}
