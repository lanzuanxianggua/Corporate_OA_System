package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_leave_apply")
public class OaLeaveApply {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long empId;

    private Integer leaveType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private String reason;

    private Integer status = 0;

    private String leavePeriod = "full";

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 员工姓名（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String empName;

    /** 审批备注（非数据库字段，取最新审批记录） */
    @TableField(exist = false)
    private String remark;
}
