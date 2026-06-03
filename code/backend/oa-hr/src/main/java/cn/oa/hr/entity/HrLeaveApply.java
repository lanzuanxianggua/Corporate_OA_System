package cn.oa.hr.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * HR请假申请表
 * 对应表: hr_leave_apply
 *
 * @author oa-hr
 */
@Data
@TableName("hr_leave_apply")
public class HrLeaveApply {

    /**
     * 请假申请ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 申请单号
     */
    private String applyNo;

    /**
     * 申请员工ID
     */
    private Long empId;

    /**
     * 申请人部门ID
     */
    private Long deptId;

    /**
     * 假期类型(PERSONAL/ANNUAL/SICK/MARRIAGE/FUNERAL/MATERNITY/PATERNITY/COMPENSATORY/OTHER)
     */
    private String leaveType;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 请假时段(FULL/AM/PM)
     */
    private String leavePeriod;

    /**
     * 请假天数
     */
    private BigDecimal days;

    /**
     * 请假原因
     */
    private String reason;

    /**
     * 附件列表JSON
     */
    private String attachments;

    /**
     * 状态(DRAFT/RUNNING/PASSED/REJECTED/REVOKED)
     */
    private String status;

    /**
     * 工作流实例ID
     */
    private Long processInstanceId;

    /**
     * 当前审批任务ID
     */
    private Long currentTaskId;

    /**
     * 审批通过时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedTime;

    /**
     * 驳回原因
     */
    private String rejectReason;

    /**
     * 删除标志(0存在 1删除)
     */
    @TableLogic
    private String delFlag;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // ============ 非数据库字段 ============

    /**
     * 申请人姓名（关联查询）
     */
    @TableField(exist = false)
    private String empName;

    /**
     * 部门名称（关联查询）
     */
    @TableField(exist = false)
    private String deptName;

    /**
     * 假期类型名称（字典翻译）
     */
    @TableField(exist = false)
    private String leaveTypeName;

    /**
     * 状态名称（字典翻译）
     */
    @TableField(exist = false)
    private String statusName;
}
