package cn.oa.system.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 部门.
 */
@Schema(description = "部门")
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    @Schema(description = "父部门ID, 0=根")
    private Long parentId;

    @Schema(description = "部门编码")
    private String deptCode;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "路径, e.g. /1/3/7/")
    private String path;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "负责人 empId")
    private Long leaderId;

    @Schema(description = "电话")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "状态: ACTIVE/INACTIVE")
    private String status;

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getDeptCode() { return deptCode; }
    public void setDeptCode(String deptCode) { this.deptCode = deptCode; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Long getLeaderId() { return leaderId; }
    public void setLeaderId(Long leaderId) { this.leaderId = leaderId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
