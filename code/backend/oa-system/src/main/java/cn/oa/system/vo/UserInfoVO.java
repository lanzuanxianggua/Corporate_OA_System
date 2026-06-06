package cn.oa.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 当前用户信息 VO.
 */
@Data
@Schema(description = "当前用户信息")
public class UserInfoVO {

    @Schema(description = "员工 ID (与前端 id 等价)")
    private Long id;

    @Schema(description = "工号")
    private String empCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "部门 ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "数据权限范围: ALL/DEPT_DOWN/COMPANY/DEPT/SELF")
    private String dataScope;

    @Schema(description = "角色编码列表")
    private List<String> roles;

    @Schema(description = "权限码列表")
    private List<String> permissions;
}
