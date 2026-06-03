package cn.oa.meeting;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mt_room")
public class MtRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roomName;

    private String location;

    private Integer capacity;

    private String devices;

    private BigDecimal gpsLat;

    private BigDecimal gpsLng;

    private String status = "0";

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}