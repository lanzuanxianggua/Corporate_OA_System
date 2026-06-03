package cn.oa.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 印章VO
 *
 * @author oa-admin
 */
@Data
public class AdmSealVO {

    private Long id;
    private String sealName;
    private String sealType;
    private Long keeperId;
    private String keeperName;
    private String status;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
