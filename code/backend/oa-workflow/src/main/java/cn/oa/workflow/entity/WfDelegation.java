package cn.oa.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 审批委托.
 */
@Schema(description = "审批委托")
@TableName("wf_delegations")
public class WfDelegation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromEmpId;
    private Long toEmpId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String reason;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFromEmpId() { return fromEmpId; }
    public void setFromEmpId(Long fromEmpId) { this.fromEmpId = fromEmpId; }
    public Long getToEmpId() { return toEmpId; }
    public void setToEmpId(Long toEmpId) { this.toEmpId = toEmpId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
