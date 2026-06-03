package cn.oa.integration;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private String empName;

    private String module;

    private String operation;

    private String method;

    private String requestUrl;

    private String requestParams;

    private String ip;

    private String status;

    private Long costTime;

    private String errorMsg;

    private LocalDateTime createdAt;
}