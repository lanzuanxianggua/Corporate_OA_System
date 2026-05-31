package cn.oa.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class HandleAlertDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "处理备注不能为空")
    private String handleRemark;
}
