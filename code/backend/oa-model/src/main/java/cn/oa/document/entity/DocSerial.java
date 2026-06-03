package cn.oa.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("doc_serial")
public class DocSerial {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orgCode;

    private Integer year;

    private Integer serialNo;

    private String status;

    private String lockedBy;

    private LocalDateTime lockedAt;

    private LocalDateTime usedAt;
}