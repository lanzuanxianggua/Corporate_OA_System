package cn.oa.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 流程实例.
 */
@Schema(description = "流程实例")
@TableName("wf_instances")
public class WfInstance {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long defId;
    private String defKey;
    private String businessKey;
    private Long initiatorId;
    private String status;
    private Long currentNodeId;
    private String vars;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDefId() { return defId; }
    public void setDefId(Long defId) { this.defId = defId; }
    public String getDefKey() { return defKey; }
    public void setDefKey(String defKey) { this.defKey = defKey; }
    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }
    public Long getInitiatorId() { return initiatorId; }
    public void setInitiatorId(Long initiatorId) { this.initiatorId = initiatorId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(Long currentNodeId) { this.currentNodeId = currentNodeId; }
    public String getVars() { return vars; }
    public void setVars(String vars) { this.vars = vars; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
