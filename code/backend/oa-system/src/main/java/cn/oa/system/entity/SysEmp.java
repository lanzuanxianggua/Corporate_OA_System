package cn.oa.system.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 员工/用户.
 */
@Schema(description = "员工")
@TableName("sys_employee")
public class SysEmp extends BaseEntity {

    @Schema(description = "工号")
    @com.baomidou.mybatisplus.annotation.TableField(value = "emp_no")
    private String empCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码(Bcrypt)")
    private String password;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "昵称")
    @com.baomidou.mybatisplus.annotation.TableField(value = "nickname", exist = false)
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "岗位")
    private String position;

    @Schema(description = "状态: ACTIVE/INACTIVE/LOCKED")
    private String status;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    public String getEmpCode() { return empCode; }
    public void setEmpCode(String empCode) { this.empCode = empCode; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
}
