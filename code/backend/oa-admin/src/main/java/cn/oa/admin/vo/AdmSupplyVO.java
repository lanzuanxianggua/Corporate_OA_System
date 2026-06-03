package cn.oa.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 办公用品VO
 *
 * @author oa-admin
 */
@Data
public class AdmSupplyVO {

    private Long id;
    private String supplyName;
    private String specification;
    private String unit;
    private String category;

    // 库存信息
    private Integer totalQty;
    private Integer availableQty;
    private Integer lockedQty;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 是否低于预警阈值 */
    private Boolean lowStock;
}
