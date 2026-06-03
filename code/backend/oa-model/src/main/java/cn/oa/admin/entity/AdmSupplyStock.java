package cn.oa.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("adm_supply_stock")
public class AdmSupplyStock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long supplyId;

    private Integer totalQuantity;

    private Integer availableQuantity;

    private Integer lockedQuantity;

    private Integer safetyStock;

    @Version
    private Integer version;
}