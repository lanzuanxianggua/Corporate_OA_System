package cn.oa.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 固定资产VO
 *
 * @author oa-admin
 */
@Data
public class AdmAssetVO {

    private Long id;
    private String assetCode;
    private String assetName;
    private String sn;
    private String brand;
    private String model;
    private String category;
    private String status;
    private String statusName;
    private Long currentUserId;
    private String currentUserName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    private BigDecimal price;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
