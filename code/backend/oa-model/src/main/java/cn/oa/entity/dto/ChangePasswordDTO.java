package cn.oa.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class ChangePasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long empId;

    @NotBlank(message = "原密码不能为空")
    private String oldPwd;

    @NotBlank(message = "新密码不能为空")
    private String newPwd;
}
