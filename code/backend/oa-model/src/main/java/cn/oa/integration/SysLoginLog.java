package cn.oa.integration;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private String username;

    private String ip;

    private String browser;

    private String os;

    private String status;

    private String message;

    private LocalDateTime loginTime;
}