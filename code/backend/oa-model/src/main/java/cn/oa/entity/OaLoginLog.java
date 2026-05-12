package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_login_log")
public class OaLoginLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long empId;
    private String username;
    private String ip;
    private String browser;
    private String os;
    private Integer status = 1;
    private String message;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime loginTime;
}
