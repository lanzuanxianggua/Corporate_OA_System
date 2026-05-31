package cn.oa.entity.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class EmpIdsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "员工ID列表不能为空")
    private List<Long> empIds;
}
