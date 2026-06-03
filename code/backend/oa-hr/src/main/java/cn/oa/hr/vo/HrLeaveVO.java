package cn.oa.hr.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * HR请假申请VO
 *
 * @author oa-hr
 */
@Data
public class HrLeaveVO {

    /**
     * 申请ID
     */
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
     * 申请人姓名
     */
    private String empName;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 假期类型code
     */
    private String leaveType;

    /**
     * 假期类型名称
     */
    private String leaveTypeName;

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
     * 请假时段
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
     * 状态code
     */
    private String status;

    /**
     * 状态名称
     */
    private String statusName;

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
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 当前用户是否可撤回
     */
    private Boolean canRevoke;

    /**
     * 当前用户是否可重提
     */
    private Boolean canResubmit;
}
