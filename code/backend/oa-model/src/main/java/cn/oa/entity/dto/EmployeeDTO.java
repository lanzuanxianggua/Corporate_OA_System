package cn.oa.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Data
public class EmployeeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "员工编号不能为空")
    private String empCode;

    @NotBlank(message = "员工姓名不能为空")
    private String empName;

    private String password;

    @Pattern(regexp = "^(|1[3-9]\\d{9})$", message = "手机号格式不正确")
    private String phone;

    @Pattern(regexp = "^(|[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,})$", message = "邮箱格式不正确")
    private String email;

    private Long deptId;

    private String avatar;

    private Integer status;

    private Long postId;
}
