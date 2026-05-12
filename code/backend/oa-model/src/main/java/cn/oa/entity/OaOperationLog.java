package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_operation_log")
public class OaOperationLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long empId;
    private String empName;
    private String module;
    private String operation;
    private String method;
    private String requestUrl;
    private String ip;
    private Integer status = 1;
    private Long costTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
