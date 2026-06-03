package cn.oa.document.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文号VO
 *
 * @author oa-document
 */
@Data
public class DocSerialVO {

    private Long id;

    /** 发文机关代字 */
    private String orgCode;

    /** 年份 */
    private Integer year;

    /** 当前流水号 */
    private Integer serialNo;

    /** 状态: ACTIVE-可用 LOCKED-已锁定 */
    private String status;

    /** 状态名称 */
    private String statusName;

    /** 锁定人ID */
    private Long lockedBy;

    /** 锁定时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lockedAt;

    /** 使用时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime usedAt;

    /** 完整文号格式 */
    private String fullSerialNo;
}
