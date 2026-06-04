package cn.oa.workflow.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 流程定义.
 */
@Schema(description = "流程定义")
@TableName("wf_definitions")
public class WfDefinition extends BaseEntity {

    private String defKey;
    private String defName;
    private Integer version;
    private String status;
    private String description;
    private Long createEmp;

    public String getDefKey() { return defKey; }
    public void setDefKey(String defKey) { this.defKey = defKey; }
    public String getDefName() { return defName; }
    public void setDefName(String defName) { this.defName = defName; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCreateEmp() { return createEmp; }
    public void setCreateEmp(Long createEmp) { this.createEmp = createEmp; }
}
