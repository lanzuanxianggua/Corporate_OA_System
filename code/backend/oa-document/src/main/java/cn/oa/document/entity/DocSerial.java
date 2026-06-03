package cn.oa.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文号表
 * 对应表: doc_serial
 * 注：此表无 delFlag 逻辑删除字段
 *
 * @author oa-document
 */
@Data
@TableName("doc_serial")
public class DocSerial {

    /** 文号ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发文机关代字 */
    private String orgCode;

    /** 年份 */
    private Integer year;

    /** 当前流水号 */
    private Integer serialNo;

    /** 状态: ACTIVE-可用 LOCKED-已锁定 */
    private String status;

    /** 锁定人ID */
    private Long lockedBy;

    /** 锁定时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lockedAt;

    /** 使用时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime usedAt;
}
