package cn.oa.system.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 权限码.
 */
@Schema(description = "权限码")
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    @Schema(description = "父权限ID, 0=根")
    private Long parentId;

    @Schema(description = "权限码, e.g. hr-leave:leave:list")
    private String permCode;

    @Schema(description = "权限名称")
    private String permName;

    @Schema(description = "类型: MENU/BUTTON/API/DATA")
    private String permType;

    @Schema(description = "路径, e.g. /api/v1/hr/leaves")
    private String path;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态: ACTIVE/INACTIVE")
    private String status;

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getPermCode() { return permCode; }
    public void setPermCode(String permCode) { this.permCode = permCode; }
    public String getPermName() { return permName; }
    public void setPermName(String permName) { this.permName = permName; }
    public String getPermType() { return permType; }
    public void setPermType(String permType) { this.permType = permType; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
