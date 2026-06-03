package cn.oa.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资产操作日志VO
 *
 * @author oa-admin
 */
@Data
public class AdmAssetLogVO {

    private Long id;
    private Long assetId;
    private String operation;
    private Long operatorId;
    private String operatorName;
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
    private String toUserName;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
