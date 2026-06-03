package cn.oa.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 印章使用记录VO
 *
 * @author oa-admin
 */
@Data
public class AdmSealUsageVO {

    private Long id;
    private Long sealId;
    private String sealName;
    private Long applicantId;
    private String applicantName;
    private String documentName;
    private Integer usageCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime usageDate;

    private String purpose;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
